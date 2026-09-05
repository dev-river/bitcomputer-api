package com.bitcomputer.portal.bgc;

import com.bitcomputer.portal.employee.EmployeeMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class BgcSeedDataInitializerTest {

    @Autowired private BackgroundCheckMapper backgroundCheckMapper;
    @Autowired private EmployeeMapper employeeMapper;

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
}
