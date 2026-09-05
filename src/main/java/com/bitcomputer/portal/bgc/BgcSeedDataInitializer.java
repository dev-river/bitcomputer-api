package com.bitcomputer.portal.bgc;

import com.bitcomputer.portal.employee.EmployeeMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BgcSeedDataInitializer implements ApplicationRunner {
    // No @Order needed relative to EmployeeSeedDataInitializer: this runner only needs EMP-001's
    // employee row to exist (true immediately after Flyway runs), not its date_of_birth backfill,
    // so the two ApplicationRunners have no ordering dependency on each other.

    private static final String DEMO_CHECK_ID = "CHK-seed-demo-0001";

    private final BackgroundCheckMapper backgroundCheckMapper;
    private final EmployeeMapper employeeMapper;

    public BgcSeedDataInitializer(BackgroundCheckMapper backgroundCheckMapper, EmployeeMapper employeeMapper) {
        this.backgroundCheckMapper = backgroundCheckMapper;
        this.employeeMapper = employeeMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        var employee = employeeMapper.findByEmpNo("EMP-001");
        if (employee == null) {
            return;
        }
        List<BackgroundCheck> history = backgroundCheckMapper.findByEmployeeId(employee.getId());
        history.stream()
            .filter(c -> DEMO_CHECK_ID.equals(c.getExternalCheckId()))
            .filter(c -> c.getCriminalRecord() == null && c.getMaskedAt() == null)
            .findFirst()
            .ifPresent(demoRow -> backgroundCheckMapper.updateAfterPollSuccess(
                demoRow.getId(), "CLEAR", "false", "true", "true", "good", demoRow.getCompletedAt()));
    }
}
