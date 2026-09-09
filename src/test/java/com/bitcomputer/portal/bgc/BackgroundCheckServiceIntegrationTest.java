package com.bitcomputer.portal.bgc;

import com.bitcomputer.portal.common.PortalException;
import com.bitcomputer.portal.employee.EmployeeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@SpringBootTest
@ActiveProfiles("test")
// Transactional so this class's background_check inserts (EMP-002, EMP-003, EMP-007, via
// triggerForEmployee/triggerManualRerun) roll back after each test, instead of leaking into the
// shared H2 instance and affecting other tests' findByEmployeeId(...)/hasSize(1) assertions for
// those employees. Same rollback pattern already used elsewhere on this branch.
@Transactional
class BackgroundCheckServiceIntegrationTest {

    @Autowired private BackgroundCheckService backgroundCheckService;
    @Autowired private BackgroundCheckMapper backgroundCheckMapper;
    @Autowired private EmployeeMapper employeeMapper;
    @Autowired private RestTemplate bgcRestTemplate;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(bgcRestTemplate);
    }

    @Test
    void triggerForEmployee_success_insertsPendingRowWithExternalCheckId() {
        int employeeId = employeeMapper.findByEmpNo("EMP-002").getId();
        mockServer.expect(requestTo("http://localhost:9999/background-checks"))
            .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                .body("{\"checkId\":\"CHK-svc-1\",\"status\":\"pending\",\"estimatedCompletionSeconds\":20}"));

        backgroundCheckService.triggerForEmployee(employeeId, 1);

        List<BackgroundCheck> history = backgroundCheckMapper.findByEmployeeId(employeeId);
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getStatus()).isEqualTo("PENDING");
        assertThat(history.get(0).getExternalCheckId()).isEqualTo("CHK-svc-1");
    }

    @Test
    void triggerForEmployee_emp007WithNullBirthDate_recordsErrorRowInsteadOfThrowing() {
        int employeeId = employeeMapper.findByEmpNo("EMP-007").getId();
        mockServer.expect(requestTo("http://localhost:9999/background-checks"))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":\"Bad Request\",\"message\":\"Missing required field: dateOfBirth\",\"statusCode\":400}"));

        backgroundCheckService.triggerForEmployee(employeeId, 1);

        List<BackgroundCheck> history = backgroundCheckMapper.findByEmployeeId(employeeId);
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getStatus()).isEqualTo("ERROR");
        assertThat(history.get(0).getErrorMessage()).contains("400");
    }

    @Test
    void triggerManualRerun_whenPendingExists_throwsDuplicateWithoutCallingApi() {
        int employeeId = employeeMapper.findByEmpNo("EMP-003").getId();
        BackgroundCheck existing = new BackgroundCheck();
        existing.setEmployeeId(employeeId);
        existing.setStatus("PENDING");
        existing.setRequestedAt(java.time.LocalDateTime.now());
        existing.setTriggeredByAccountId(1);
        backgroundCheckMapper.insert(existing);

        assertThrows(PortalException.class, () -> backgroundCheckService.triggerManualRerun(employeeId, 1));
        mockServer.verify(); // no HTTP expectations were set, so this also proves no call was made
    }

    @Test
    void submit_staysAnnotatedAsync() throws NoSuchMethodException {
        // Guards against silently losing the @Async annotation on BackgroundCheckAsyncRunner.submit
        // (MEASUREMENTS.md §4-1 fix) — a regression there wouldn't fail any other test, since
        // TestAsyncConfig makes async and sync execution produce identical observable results here.
        var method = BackgroundCheckAsyncRunner.class.getMethod(
            "submit", int.class, String.class, NameSplitter.SplitName.class, String.class);
        assertThat(method.isAnnotationPresent(org.springframework.scheduling.annotation.Async.class)).isTrue();
    }
}
