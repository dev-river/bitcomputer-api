package com.bitcomputer.portal.bgc;

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
// Transactional because applicationStartup_doesNotReBackfillAlreadyMaskedRow masks the shared V4
// seed row (EMP-001's CHK-seed-demo-0001) directly via the mapper — without rollback that would
// permanently mask the row in the shared H2 instance and break other tests (e.g.
// AdminBackgroundCheckControllerIntegrationTest.history_returnsSeededMaskingDemoRecordForEmp001,
// BackgroundCheckMapperIntegrationTest.findMaskCandidates_returnsOnlyOldCompletedUnmaskedChecks)
// that assume the demo row is still unmasked. Same rollback pattern already used elsewhere on
// this branch (see AccountMapperIntegrationTest, BackgroundCheckMaskingSchedulerTest, etc.).
@Transactional
class BgcSeedDataInitializerTest {

    @Autowired private BackgroundCheckMapper backgroundCheckMapper;
    @Autowired private EmployeeMapper employeeMapper;
    @Autowired private BgcSeedDataInitializer bgcSeedDataInitializer;

    @Test
    void applicationStartup_backfillsMaskingDemoRowEncryptedFields() {
        int emp001Id = employeeMapper.findByEmpNo("EMP-001").getId();
        List<BackgroundCheck> history = backgroundCheckMapper.findByEmployeeId(emp001Id);
        BackgroundCheck demoRow = history.stream()
            .filter(c -> "CHK-seed-demo-0001".equals(c.getExternalCheckId()))
            .findFirst()
            .orElseThrow();

        assertThat(demoRow.getCriminalRecord()).isEqualTo("false");
        assertThat(demoRow.getEducationVerified()).isEqualTo("true");
        assertThat(demoRow.getEmploymentVerified()).isEqualTo("true");
        assertThat(demoRow.getCreditScore()).isEqualTo("good");
        assertThat(demoRow.getStatus()).isEqualTo("CLEAR");
        assertThat(demoRow.getCompletedAt()).isNotNull();
    }

    @Test
    void applicationStartup_doesNotReBackfillAlreadyMaskedRow() {
        // Regression test for the bug where the initializer's backfill guard
        // (criminalRecord == null) was indistinguishable from a row the 90-day masking scheduler
        // had already cleared. Simulate that: mask the demo row directly (as the scheduler would),
        // then re-invoke the initializer's run() a second time (ApplicationArguments is unused in
        // the method body, so passing null is safe) and assert it does NOT repopulate the
        // sensitive fields it just wiped.
        int emp001Id = employeeMapper.findByEmpNo("EMP-001").getId();
        BackgroundCheck demoRow = backgroundCheckMapper.findByEmployeeId(emp001Id).stream()
            .filter(c -> "CHK-seed-demo-0001".equals(c.getExternalCheckId()))
            .findFirst()
            .orElseThrow();

        backgroundCheckMapper.maskSensitiveFields(demoRow.getId(), LocalDateTime.now());

        BackgroundCheck maskedRow = backgroundCheckMapper.findById(demoRow.getId());
        assertThat(maskedRow.getCriminalRecord()).isNull();
        assertThat(maskedRow.getMaskedAt()).isNotNull();

        bgcSeedDataInitializer.run(null);

        BackgroundCheck afterSecondRun = backgroundCheckMapper.findById(demoRow.getId());
        assertThat(afterSecondRun.getCriminalRecord()).isNull();
        assertThat(afterSecondRun.getMaskedAt()).isNotNull();
    }
}
