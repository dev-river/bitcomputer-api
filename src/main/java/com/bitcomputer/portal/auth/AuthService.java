package com.bitcomputer.portal.auth;

import com.bitcomputer.portal.account.Account;
import com.bitcomputer.portal.account.AccountMapper;
import com.bitcomputer.portal.auth.dto.LoginResponse;
import com.bitcomputer.portal.common.ErrorCode;
import com.bitcomputer.portal.common.PortalException;
import com.bitcomputer.portal.employee.Employee;
import com.bitcomputer.portal.employee.EmployeeMapper;
import com.bitcomputer.portal.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AccountMapper accountMapper;
    private final EmployeeMapper employeeMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(AccountMapper accountMapper, EmployeeMapper employeeMapper, PasswordEncoder passwordEncoder,
                        JwtTokenProvider jwtTokenProvider) {
        this.accountMapper = accountMapper;
        this.employeeMapper = employeeMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public LoginResponse login(String loginId, String rawPassword) {
        Account account = accountMapper.findByLoginId(loginId);
        if (account == null || !passwordEncoder.matches(rawPassword, account.getPasswordHash())) {
            throw new PortalException(ErrorCode.ERR_UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다");
        }
        if (account.getEmployeeId() != null) {
            Employee employee = employeeMapper.findById(account.getEmployeeId());
            if (employee == null || "TERMINATED".equals(employee.getStatus())) {
                throw new PortalException(ErrorCode.ERR_FORBIDDEN, "퇴사 처리된 계정입니다");
            }
        }
        String token = jwtTokenProvider.generateToken(account.getId(), account.getRole(), account.getEmployeeId());
        return new LoginResponse(token, account.isMustChangePassword());
    }

    public void changePassword(int accountId, String currentPassword, String newPassword) {
        Account account = accountMapper.findById(accountId);
        if (account == null || !passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            throw new PortalException(ErrorCode.ERR_UNAUTHORIZED, "현재 비밀번호가 올바르지 않습니다");
        }
        accountMapper.updatePasswordAndClearMustChange(accountId, passwordEncoder.encode(newPassword));
    }
}
