package com.bitcomputer.portal.employee;

import com.bitcomputer.portal.crypto.AesCryptoUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class EmployeeSeedDataInitializerTest {

    @Autowired private EmployeeMapper employeeMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private AesCryptoUtil aesCryptoUtil;

    @Test
    void applicationStartup_backfillsAllRealBirthDatesExceptEmp007() {
        // The ApplicationRunner already ran once when the shared test context booted (before this
        // test method executes), so by now every non-EMP-007 employee should have a decryptable,
        // correct date_of_birth, and EMP-007's must still be NULL.
        assertThat(employeeMapper.findByEmpNo("EMP-001").getDateOfBirth()).isEqualTo("1990-03-15");
        assertThat(employeeMapper.findByEmpNo("EMP-006").getDateOfBirth()).isEqualTo("1991-05-05");
        assertThat(employeeMapper.findByEmpNo("EMP-010").getDateOfBirth()).isEqualTo("1989-10-11");
        assertThat(employeeMapper.findByEmpNo("EMP-007").getDateOfBirth()).isNull();

        String rawColumn = jdbcTemplate.queryForObject(
            "SELECT date_of_birth FROM employee WHERE emp_no = 'EMP-006'", String.class);
        assertThat(rawColumn).isNotEqualTo("1991-05-05");
        assertThat(aesCryptoUtil.decrypt(rawColumn)).isEqualTo("1991-05-05");
    }
}
