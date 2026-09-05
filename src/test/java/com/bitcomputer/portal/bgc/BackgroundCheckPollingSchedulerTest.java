package com.bitcomputer.portal.bgc;

import com.bitcomputer.portal.employee.EmployeeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@SpringBootTest
@ActiveProfiles("test")
class BackgroundCheckPollingSchedulerTest {

    @Autowired private BackgroundCheckPollingScheduler scheduler;
    @Autowired private BackgroundCheckMapper backgroundCheckMapper;
    @Autowired private EmployeeMapper employeeMapper;
    @Autowired private RestTemplate bgcRestTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(bgcRestTemplate);
    }

    private BackgroundCheck insertPending(String empNo, String externalCheckId, int retryCount) {
        int employeeId = employeeMapper.findByEmpNo(empNo).getId();
        BackgroundCheck check = new BackgroundCheck();
        check.setEmployeeId(employeeId);
        check.setExternalCheckId(externalCheckId);
        check.setStatus("PENDING");
        check.setRequestedAt(LocalDateTime.now());
        check.setTriggeredByAccountId(1);
        backgroundCheckMapper.insert(check);
        if (retryCount > 0) {
            jdbcTemplate.update("UPDATE background_check SET retry_count = ? WHERE id = ?", retryCount, check.getId());
        }
        return backgroundCheckMapper.findById(check.getId());
    }

    @Test
    void pollOne_stillPending_leavesRowUnchanged() {
        BackgroundCheck check = insertPending("EMP-004", "CHK-poll-1", 0);
        mockServer.expect(requestTo("http://localhost:9999/background-checks/CHK-poll-1"))
            .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                .body("{\"checkId\":\"CHK-poll-1\",\"status\":\"pending\"}"));

        scheduler.pollOne(check);

        assertThat(backgroundCheckMapper.findById(check.getId()).getStatus()).isEqualTo("PENDING");
    }

    @Test
    void pollOne_becomesClear_updatesResultAndCompletedAt() {
        BackgroundCheck check = insertPending("EMP-005", "CHK-poll-2", 0);
        mockServer.expect(requestTo("http://localhost:9999/background-checks/CHK-poll-2"))
            .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                .body("{\"checkId\":\"CHK-poll-2\",\"status\":\"clear\",\"criminalRecord\":false,\"educationVerified\":true,\"employmentVerified\":true,\"creditScore\":\"good\",\"completedAt\":\"2025-01-15T09:31:45Z\"}"));

        scheduler.pollOne(check);

        BackgroundCheck reloaded = backgroundCheckMapper.findById(check.getId());
        assertThat(reloaded.getStatus()).isEqualTo("CLEAR");
        assertThat(reloaded.getCriminalRecord()).isEqualTo("false");
        assertThat(reloaded.getCompletedAt()).isNotNull();
    }

    @Test
    void pollOne_repeatedFailureBelowThreshold_incrementsRetryCountButStaysPending() {
        BackgroundCheck check = insertPending("EMP-006", "CHK-poll-3", 0);
        // bgc.retry-count=2 default in application.yml → the client itself makes 3 attempts per pollOne() call
        for (int i = 0; i < 3; i++) {
            mockServer.expect(requestTo("http://localhost:9999/background-checks/CHK-poll-3"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        }

        scheduler.pollOne(check);

        BackgroundCheck reloaded = backgroundCheckMapper.findById(check.getId());
        assertThat(reloaded.getStatus()).isEqualTo("PENDING");
        assertThat(reloaded.getRetryCount()).isEqualTo(1);
    }

    @Test
    void pollOne_failureAtThreshold_transitionsToError() {
        // application.yml default bgc.max-poll-retry-count=20 (placeholder) — seed retryCount one below it
        BackgroundCheck check = insertPending("EMP-008", "CHK-poll-4", 19);
        for (int i = 0; i < 3; i++) {
            mockServer.expect(requestTo("http://localhost:9999/background-checks/CHK-poll-4"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        }

        scheduler.pollOne(check);

        assertThat(backgroundCheckMapper.findById(check.getId()).getStatus()).isEqualTo("ERROR");
    }
}
