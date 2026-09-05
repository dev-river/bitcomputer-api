package com.bitcomputer.portal.bgc;

import com.bitcomputer.portal.bgc.dto.BgcCreateRequest;
import com.bitcomputer.portal.bgc.dto.BgcCreateResponse;
import com.bitcomputer.portal.bgc.dto.BgcResultResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.function.Supplier;

@Component
public class BackgroundCheckApiClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final int retryCount;
    private final long retryIntervalMs;

    public BackgroundCheckApiClient(RestTemplate bgcRestTemplate,
                                     @Value("${bgc.base-url}") String baseUrl,
                                     @Value("${bgc.retry-count}") int retryCount,
                                     @Value("${bgc.retry-interval-ms}") long retryIntervalMs) {
        this.restTemplate = bgcRestTemplate;
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
                sleep(retryIntervalMs);
            }
        }
        throw new BackgroundCheckApiException(describeError(lastError), lastError);
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
