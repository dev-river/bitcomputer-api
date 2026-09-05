package com.bitcomputer.portal.bgc;

import com.bitcomputer.portal.bgc.dto.BgcCreateRequest;
import com.bitcomputer.portal.bgc.dto.BgcCreateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

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
        // application.yml default bgc.retry-count=2 → 1 initial attempt + 2 retries = 3 total calls
        for (int i = 0; i < 3; i++) {
            mockServer.expect(requestTo("http://localhost:9999/background-checks"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"Service Unavailable\",\"retryAfter\":30,\"statusCode\":503}"));
        }

        BackgroundCheckApiException ex = assertThrows(BackgroundCheckApiException.class,
            () -> client.create(new BgcCreateRequest("EMP-005", "민준", "김", "1990-03-15")));

        assertThat(ex.getMessage()).contains("503");
        mockServer.verify();
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
