package com.bitcomputer.portal.account;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
// Transactional so insert_generatesIdAndPersists's permanent-looking EMP-005 account insert rolls
// back after the test — without this, it leaked into the shared H2 instance for the rest of the
// suite and collided with Task 8's AdminAccountControllerIntegrationTest, which assumes EMP-005
// has no account yet. Found while making Task 8's tests pass across the full suite (not just in
// isolation); not part of the original Task 8 brief, but needed to keep `./gradlew test` green.
@Transactional
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
