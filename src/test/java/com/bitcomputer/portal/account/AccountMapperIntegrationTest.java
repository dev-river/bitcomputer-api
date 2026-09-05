package com.bitcomputer.portal.account;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AccountMapperIntegrationTest {

    @Autowired
    private AccountMapper accountMapper;

    @Test
    void findByLoginId_returnsSeededAdmin() {
        Account account = accountMapper.findByLoginId("ADMIN-001");
        assertThat(account.getRole()).isEqualTo("ADMIN");
        assertThat(account.getEmployeeId()).isNull();
    }

    @Test
    void insert_generatesIdAndPersists() {
        Account newAccount = new Account();
        newAccount.setLoginId("EMP-005");
        newAccount.setPasswordHash("hashed");
        newAccount.setRole("EMPLOYEE");
        newAccount.setEmployeeId(5);
        newAccount.setMustChangePassword(true);
        newAccount.setCreatedByAccountId(1);

        accountMapper.insert(newAccount);

        assertThat(newAccount.getId()).isNotNull();
        Account reloaded = accountMapper.findByLoginId("EMP-005");
        assertThat(reloaded.getCreatedByAccountId()).isEqualTo(1);
    }
}
