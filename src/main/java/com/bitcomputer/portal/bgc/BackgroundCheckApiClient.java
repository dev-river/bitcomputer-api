package com.bitcomputer.portal.bgc;

import com.bitcomputer.portal.bgc.dto.BgcCreateRequest;
import com.bitcomputer.portal.bgc.dto.BgcCreateResponse;
import com.bitcomputer.portal.bgc.dto.BgcResultResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.function.Supplier;

@Component
public class BackgroundCheckApiClient {

    // MEASUREMENTS.md §3 실측: 이 API는 swagger 명세와 달리 Retry-After를 HTTP 헤더로 내려주지
    // 않는다(헤더는 매번 비어 있었음) — 대신 503 응답 바디의 retryAfter 필드에 초 단위로 담아 보낸다.
    // 서버가 비정상적으로 큰 값을 보내도 무한정 대기하지 않도록 상한을 둔다.
    private static final long MAX_RETRY_AFTER_MS = 120_000;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final int retryCount;
    private final long retryIntervalMs;

    public BackgroundCheckApiClient(RestTemplate bgcRestTemplate,
                                     ObjectMapper objectMapper,
                                     @Value("${bgc.base-url}") String baseUrl,
                                     @Value("${bgc.retry-count}") int retryCount,
                                     @Value("${bgc.retry-interval-ms}") long retryIntervalMs) {
        this.restTemplate = bgcRestTemplate;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.retryCount = retryCount;
        this.retryIntervalMs = retryIntervalMs;
    }

    public BgcCreateResponse create(BgcCreateRequest request) {
        return executeWithRetry(() -> restTemplate.postForObject(baseUrl + "/background-checks", request, BgcCreateResponse.class));
    }

    public BgcResultResponse get(String checkId) {
        return executeWithRetry(() -> restTemplate.getForObject(baseUrl + "/background-checks/{checkId}", BgcResultResponse.class, checkId));
    }

    private <T> T executeWithRetry(Supplier<T> action) {
        RestClientException lastError = null;
        for (int attempt = 0; attempt <= retryCount; attempt++) {
            try {
                return action.get();
            } catch (RestClientException e) {
                lastError = e;
                if (!isRetryable(e) || attempt >= retryCount) {
                    break;
                }
                sleep(resolveWaitMs(e));
            }
        }
        throw new BackgroundCheckApiException(describeError(lastError), lastError);
    }

    /**
     * Retry-After 값을 구한다: 명세대로 헤더를 먼저 확인하고(관측상 항상 비어 있었지만, 서버가
     * 나중에 명세대로 고치면 자연히 동작하도록), 없으면 응답 바디의 retryAfter 필드를 읽는다.
     * 둘 다 없거나 파싱할 수 없으면 설정된 고정 간격으로 대체한다.
     */
    long resolveWaitMs(RestClientException e) {
        if (!(e instanceof RestClientResponseException responseEx)) {
            return retryIntervalMs;
        }
        Long headerSeconds = parseSeconds(
            responseEx.getResponseHeaders() != null ? responseEx.getResponseHeaders().getFirst("Retry-After") : null);
        if (headerSeconds != null) {
            return capMs(headerSeconds);
        }
        try {
            JsonNode body = objectMapper.readTree(responseEx.getResponseBodyAsString());
            if (body.hasNonNull("retryAfter")) {
                return capMs(body.get("retryAfter").asLong());
            }
        } catch (Exception ignored) {
            // 바디가 JSON이 아니거나 파싱 실패 — 아래에서 고정 간격으로 대체
        }
        return retryIntervalMs;
    }

    private long capMs(long seconds) {
        return Math.min(seconds * 1000, MAX_RETRY_AFTER_MS);
    }

    private Long parseSeconds(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Only 5xx responses and network-level failures (timeout, connection refused — anything that
     * isn't a {@link RestClientResponseException}) are retried. A 4xx means the request itself was
     * invalid (e.g. EMP-007's missing dateOfBirth) — retrying it burns the retry budget on a call
     * that will never succeed, so it fails fast instead.
     */
    private boolean isRetryable(RestClientException e) {
        if (e instanceof RestClientResponseException responseEx) {
            return responseEx.getStatusCode().is5xxServerError();
        }
        return true;
    }

    private String describeError(RestClientException e) {
        if (e instanceof RestClientResponseException responseEx) {
            String body = responseEx.getResponseBodyAsString();
            return String.format("[%d] %s", responseEx.getStatusCode().value(),
                body.length() > 300 ? body.substring(0, 300) : body);
        }
        return e == null ? "unknown error" : e.getClass().getSimpleName() + ": " + e.getMessage();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
