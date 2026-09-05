package com.bitcomputer.portal.employee;

import com.bitcomputer.portal.crypto.AesCryptoUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class EmployeeMapperIntegrationTest {

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AesCryptoUtil aesCryptoUtil;

    @Test
    void findByEmpNo_returnsSeedEmployeeWithBackfilledEncryptedBirthDate() {
        // EmployeeSeedDataInitializer (Step 6) backfills EMP-001's real date_of_birth at application
        // startup, encrypting it through this same mapper — by the time this test runs (well after
        // context startup), it should already be set and decryptable.
        Employee employee = employeeMapper.findByEmpNo("EMP-001");
        assertThat(employee.getDateOfBirth()).isEqualTo("1990-03-15");

        String rawColumn = jdbcTemplate.queryForObject(
            "SELECT date_of_birth FROM employee WHERE emp_no = 'EMP-001'", String.class);
        assertThat(rawColumn).isNotEqualTo("1990-03-15");
        assertThat(aesCryptoUtil.decrypt(rawColumn)).isEqualTo("1990-03-15");
    }

    @Test
    void updateContactInfo_encryptsPhoneAndAddress() {
        Employee employee = employeeMapper.findByEmpNo("EMP-002");
        employeeMapper.updateContactInfo(employee.getId(), "010-1234-5678", employee.getEmail(), "서울시 강남구");

        Employee reloaded = employeeMapper.findByEmpNo("EMP-002");
        assertThat(reloaded.getPhone()).isEqualTo("010-1234-5678");
        assertThat(reloaded.getAddress()).isEqualTo("서울시 강남구");

        String rawPhoneColumn = jdbcTemplate.queryForObject(
            "SELECT phone FROM employee WHERE emp_no = 'EMP-002'", String.class);
        assertThat(rawPhoneColumn).isNotEqualTo("010-1234-5678");
        assertThat(aesCryptoUtil.decrypt(rawPhoneColumn)).isEqualTo("010-1234-5678");
    }

    @Test
    void findByEmpNo_emp007_hasNullDateOfBirth() {
        // EmployeeSeedDataInitializer deliberately skips EMP-007 — its birth date is unconfirmed
        // per the task doc, so it must stay NULL even after the backfill runs.
        Employee employee = employeeMapper.findByEmpNo("EMP-007");
        assertThat(employee.getName()).isEqualTo("이서연");
        assertThat(employee.getDateOfBirth()).isNull();
    }

    @Test
    void search_filtersByNameAndStatus() {
        EmployeeSearchCriteria criteria = new EmployeeSearchCriteria();
        criteria.setName("김민준");
        assertThat(employeeMapper.search(criteria)).hasSize(2);
    }

    @Test
    void updateStatus_setsTerminatedAt() {
        Employee employee = employeeMapper.findByEmpNo("EMP-010");
        LocalDateTime now = LocalDateTime.now();
        employeeMapper.updateStatus(employee.getId(), "TERMINATED", now);

        Employee reloaded = employeeMapper.findByEmpNo("EMP-010");
        assertThat(reloaded.getStatus()).isEqualTo("TERMINATED");
        assertThat(reloaded.getTerminatedAt()).isNotNull();
    }
}
