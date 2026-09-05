package com.bitcomputer.portal.admin;

import com.bitcomputer.portal.account.Account;
import com.bitcomputer.portal.account.AccountMapper;
import com.bitcomputer.portal.admin.dto.CreateAccountResponse;
import com.bitcomputer.portal.bgc.BackgroundCheckService;
import com.bitcomputer.portal.common.ErrorCode;
import com.bitcomputer.portal.common.PortalException;
import com.bitcomputer.portal.employee.Employee;
import com.bitcomputer.portal.employee.EmployeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminAccountService {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountService.class);

    private final EmployeeService employeeService;
    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;
    private final BackgroundCheckService backgroundCheckService;

    public AdminAccountService(EmployeeService employeeService, AccountMapper accountMapper,
                                PasswordEncoder passwordEncoder, BackgroundCheckService backgroundCheckService) {
        this.employeeService = employeeService;
        this.accountMapper = accountMapper;
        this.passwordEncoder = passwordEncoder;
        this.backgroundCheckService = backgroundCheckService;
    }

    public CreateAccountResponse createAccountForEmployee(int employeeId, int createdByAccountId) {
        Employee employee = employeeService.getById(employeeId);
        if (accountMapper.findByEmployeeId(employeeId) != null) {
            throw new PortalException(ErrorCode.ERR_DUPLICATE, "이미 계정이 발급된 직원입니다: " + employee.getEmpNo());
        }

        String tempPassword = TempPasswordGenerator.generate();
        Account account = new Account();
        account.setLoginId(employee.getEmpNo());
        account.setPasswordHash(passwordEncoder.encode(tempPassword));
        account.setRole("EMPLOYEE");
        account.setEmployeeId(employeeId);
        account.setMustChangePassword(true);
        account.setCreatedByAccountId(createdByAccountId);
        accountMapper.insert(account);

        try {
            backgroundCheckService.triggerForEmployee(employeeId, createdByAccountId);
        } catch (RuntimeException e) {
            log.warn("Background check auto-trigger failed for employeeId={}", employeeId, e);
        }

        return new CreateAccountResponse(account.getLoginId(), tempPassword);
    }
}
