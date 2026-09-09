package com.bitcomputer.portal.bgc;

import com.bitcomputer.portal.bgc.dto.BgcCreateRequest;
import com.bitcomputer.portal.bgc.dto.BgcCreateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@SpringBootTest
@ActiveProfiles("test")
class BackgroundCheckApiClientTest {

    @Autowired private BackgroundCheckApiClient client;
    @Autowired private RestTemplate bgcRestTemplate;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(bgcRestTemplate);
    }

    @Test
    void create_success_parsesResponse() {
        mockServer.expect(requestTo("http://localhost:9999/background-checks"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"checkId\":\"CHK-1\",\"employeeId\":\"EMP-005\",\"status\":\"pending\",\"estimatedCompletionSeconds\":20}"));

        BgcCreateResponse response = client.create(new BgcCreateRequest("EMP-005", "민준", "김", "1990-03-15"));

        assertThat(response.getCheckId()).isEqualTo("CHK-1");
        assertThat(response.getStatus()).isEqualTo("pending");
        mockServer.verify();
    }

    @Test
    void create_persistentServerError_retriesConfiguredTimesThenThrowsWithStatusInMessage() {
        // application-test.yml pins bgc.retry-count=2 → 1 initial attempt + 2 retries = 3 total calls.
        // Body deliberately omits retryAfter so this test exercises the fast, configured
        // retry-interval-ms (10ms in application-test.yml) rather than the real ~30s the live API
        // actually sends in its retryAfter field — that behavior has its own resolveWaitMs tests below.
        for (int i = 0; i < 3; i++) {
            mockServer.expect(requestTo("http://localhost:9999/background-checks"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"Service Unavailable\",\"statusCode\":503}"));
        }

        BackgroundCheckApiException ex = assertThrows(BackgroundCheckApiException.class,
            () -> client.create(new BgcCreateRequest("EMP-005", "민준", "김", "1990-03-15")));

        assertThat(ex.getMessage()).contains("503");
        mockServer.verify();
    }

    @Test
    void resolveWaitMs_bodyHasRetryAfter_usesThatValueInMs() {
        // MEASUREMENTS.md §3: the live API sends retryAfter in the response BODY, not the
        // Retry-After header (the header was empty in every real capture) — swagger.yaml claims
        // the header, so this is the divergence the fix targets.
        RestClientResponseException ex = HttpServerErrorException.create(HttpStatusCode.valueOf(503), "Service Unavailable",
            HttpHeaders.EMPTY, "{\"error\":\"Service Unavailable\",\"retryAfter\":30,\"statusCode\":503}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        assertThat(client.resolveWaitMs(ex)).isEqualTo(30_000L);
    }

    @Test
    void resolveWaitMs_headerPresent_prefersHeaderOverBody() {
        // Never observed in practice, but if the API is ever fixed to match its own spec, the
        // header should win over the body field.
        HttpHeaders headers = new HttpHeaders();
        headers.set("Retry-After", "5");
        RestClientResponseException ex = HttpServerErrorException.create(HttpStatusCode.valueOf(503), "Service Unavailable",
            headers, "{\"retryAfter\":30}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        assertThat(client.resolveWaitMs(ex)).isEqualTo(5_000L);
    }

    @Test
    void resolveWaitMs_noRetryAfterAnywhere_fallsBackToConfiguredInterval() {
        RestClientResponseException ex = HttpServerErrorException.create(HttpStatusCode.valueOf(500), "Internal Server Error",
            HttpHeaders.EMPTY, "{\"error\":\"Internal Server Error\"}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        // application-test.yml pins bgc.retry-interval-ms=10
        assertThat(client.resolveWaitMs(ex)).isEqualTo(10L);
    }

    @Test
    void resolveWaitMs_absurdlyLargeRetryAfter_isCapped() {
        RestClientResponseException ex = HttpServerErrorException.create(HttpStatusCode.valueOf(503), "Service Unavailable",
            HttpHeaders.EMPTY, "{\"retryAfter\":999999}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        assertThat(client.resolveWaitMs(ex)).isEqualTo(120_000L);
    }

    @Test
    void resolveWaitMs_networkLevelException_fallsBackToConfiguredInterval() {
        assertThat(client.resolveWaitMs(new ResourceAccessException("timeout"))).isEqualTo(10L);
    }

    @Test
    void get_notFound_throwsWithStatusCodeInMessage() {
        mockServer.expect(requestTo("http://localhost:9999/background-checks/CHK-invalid"))
            .andRespond(withStatus(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":\"Not Found\",\"message\":\"Background check not found: CHK-invalid\",\"statusCode\":404}"));

        BackgroundCheckApiException ex = assertThrows(BackgroundCheckApiException.class, () -> client.get("CHK-invalid"));
        assertThat(ex.getMessage()).contains("404");
    }
}
