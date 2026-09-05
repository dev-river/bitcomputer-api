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
// Transactional so this test's masking of the shared V4 seed row (EMP-001's CHK-seed-demo-0001)
// rolls back afterward — without this it permanently masked that row in the shared H2 instance,
// which collided with Task 12's BgcSeedDataInitializerTest asserting the row's encrypted fields
// are still populated. Same class of cross-task pollution bug as AccountMapperIntegrationTest's
// fix (Task 8); not part of the original Task 13 brief, but needed to keep `./gradlew test` green.
@Transactional
class BackgroundCheckMaskingSchedulerTest {

    @Autowired private BackgroundCheckMaskingScheduler scheduler;
    @Autowired private BackgroundCheckMapper backgroundCheckMapper;
    @Autowired private EmployeeMapper employeeMapper;

    @Test
    void maskExpiredResults_masksSeededStaleRecord_leavesFreshRecordAlone() {
        int emp001Id = employeeMapper.findByEmpNo("EMP-001").getId();

        BackgroundCheck fresh = new BackgroundCheck();
        fresh.setEmployeeId(emp001Id);
        fresh.setStatus("CLEAR");
        fresh.setRequestedAt(LocalDateTime.now());
        fresh.setTriggeredByAccountId(1);
        backgroundCheckMapper.insert(fresh);
        backgroundCheckMapper.updateAfterPollSuccess(fresh.getId(), "CLEAR", "false", "true", "true", "good", LocalDateTime.now());

        scheduler.maskExpiredResults();

        List<BackgroundCheck> history = backgroundCheckMapper.findByEmployeeId(emp001Id);
        BackgroundCheck staleSeeded = history.stream()
            .filter(c -> "CHK-seed-demo-0001".equals(c.getExternalCheckId())).findFirst().orElseThrow();
        BackgroundCheck freshReloaded = history.stream()
            .filter(c -> c.getId().equals(fresh.getId())).findFirst().orElseThrow();

        assertThat(staleSeeded.getCriminalRecord()).isNull();
        assertThat(staleSeeded.getMaskedAt()).isNotNull();
        assertThat(staleSeeded.getStatus()).isEqualTo("CLEAR"); // status/history preserved — only sensitive fields cleared

        assertThat(freshReloaded.getCriminalRecord()).isEqualTo("false");
        assertThat(freshReloaded.getMaskedAt()).isNull();
    }
}
