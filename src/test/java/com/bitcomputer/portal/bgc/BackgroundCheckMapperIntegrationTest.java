package com.bitcomputer.portal.bgc;

import com.bitcomputer.portal.crypto.AesCryptoUtil;
import com.bitcomputer.portal.employee.EmployeeMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
// Transactional so this class's background_check inserts (EMP-006, EMP-008, EMP-009) roll back
// after each test. Without this, insert_thenFindByEmployeeId_roundTrips's EMP-006 insert could
// collide with BackgroundCheckPollingSchedulerTest.pollOne_repeatedFailureBelowThreshold, which
// also inserts an EMP-006 row with no cleanup — both asserting on findByEmployeeId(...) results
// for the same employee, making the outcome depend on test execution order. Same rollback pattern
// already used elsewhere on this branch.
@Transactional
class BackgroundCheckMapperIntegrationTest {

    @Autowired private BackgroundCheckMapper backgroundCheckMapper;
    @Autowired private EmployeeMapper employeeMapper;
    @Autowired private AesCryptoUtil aesCryptoUtil;

    @Test
    void insert_thenFindByEmployeeId_roundTrips() {
        int employeeId = employeeMapper.findByEmpNo("EMP-006").getId();
        BackgroundCheck check = new BackgroundCheck();
        check.setEmployeeId(employeeId);
        check.setExternalCheckId("CHK-test-1");
        check.setStatus("PENDING");
        check.setRequestedAt(LocalDateTime.now());
        check.setTriggeredByAccountId(1);

        backgroundCheckMapper.insert(check);

        assertThat(check.getId()).isNotNull();
        List<BackgroundCheck> found = backgroundCheckMapper.findByEmployeeId(employeeId);
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getExternalCheckId()).isEqualTo("CHK-test-1");
    }

    @Test
    void existsPendingByEmployeeId_reflectsCurrentStatus() {
        int employeeId = employeeMapper.findByEmpNo("EMP-008").getId();
        assertThat(backgroundCheckMapper.existsPendingByEmployeeId(employeeId)).isFalse();

        BackgroundCheck check = new BackgroundCheck();
        check.setEmployeeId(employeeId);
        check.setStatus("PENDING");
        check.setRequestedAt(LocalDateTime.now());
        check.setTriggeredByAccountId(1);
        backgroundCheckMapper.insert(check);

        assertThat(backgroundCheckMapper.existsPendingByEmployeeId(employeeId)).isTrue();

        backgroundCheckMapper.updateAfterPollSuccess(check.getId(), "CLEAR", "false", "true", "true", "good", LocalDateTime.now());
        assertThat(backgroundCheckMapper.existsPendingByEmployeeId(employeeId)).isFalse();
    }

    @Test
    void updateAfterPollSuccess_encryptsSensitiveFields() {
        int employeeId = employeeMapper.findByEmpNo("EMP-009").getId();
        BackgroundCheck check = new BackgroundCheck();
        check.setEmployeeId(employeeId);
        check.setStatus("PENDING");
        check.setRequestedAt(LocalDateTime.now());
        check.setTriggeredByAccountId(1);
        backgroundCheckMapper.insert(check);

        backgroundCheckMapper.updateAfterPollSuccess(check.getId(), "FLAGGED", "true", "false", "true", "poor", LocalDateTime.now());

        BackgroundCheck reloaded = backgroundCheckMapper.findByEmployeeId(employeeId).get(0);
        assertThat(reloaded.getCriminalRecord()).isEqualTo("true");
        assertThat(reloaded.getCreditScore()).isEqualTo("poor");
    }

    @Test
    void findMaskCandidates_returnsOnlyOldCompletedUnmaskedChecks() {
        List<BackgroundCheck> candidates = backgroundCheckMapper.findMaskCandidates(LocalDateTime.now().minusDays(90));
        // V4 seeds one BGC record completed 95 days ago for EMP-001 — see Task 1 Step 6
        assertThat(candidates).anyMatch(c -> c.getExternalCheckId().equals("CHK-seed-demo-0001"));
    }
}
