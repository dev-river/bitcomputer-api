package com.bitcomputer.portal.employee;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EmployeeSeedDataInitializer implements ApplicationRunner {

    private static final Map<String, String> REAL_BIRTH_DATES = Map.ofEntries(
        Map.entry("EMP-001", "1990-03-15"),
        Map.entry("EMP-002", "1994-11-02"),
        Map.entry("EMP-003", "1988-07-21"),
        Map.entry("EMP-004", "1995-02-09"),
        Map.entry("EMP-005", "1992-12-30"),
        Map.entry("EMP-006", "1991-05-05"),
        Map.entry("EMP-008", "1993-08-17"),
        Map.entry("EMP-009", "1996-04-03"),
        Map.entry("EMP-010", "1989-10-11")
    );

    private final EmployeeMapper employeeMapper;

    public EmployeeSeedDataInitializer(EmployeeMapper employeeMapper) {
        this.employeeMapper = employeeMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        REAL_BIRTH_DATES.forEach((empNo, dateOfBirth) -> {
            Employee employee = employeeMapper.findByEmpNo(empNo);
            if (employee != null && employee.getDateOfBirth() == null) {
                employeeMapper.updateDateOfBirth(employee.getId(), dateOfBirth);
            }
        });
    }
}
