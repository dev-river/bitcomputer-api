package com.bitcomputer.portal.auth;

import com.bitcomputer.portal.account.Account;
import com.bitcomputer.portal.account.AccountMapper;
import com.bitcomputer.portal.auth.dto.LoginResponse;
import com.bitcomputer.portal.common.ErrorCode;
import com.bitcomputer.portal.common.PortalException;
import com.bitcomputer.portal.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(AccountMapper accountMapper, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.accountMapper = accountMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public LoginResponse login(String loginId, String rawPassword) {
        Account account = accountMapper.findByLoginId(loginId);
        if (account == null || !passwordEncoder.matches(rawPassword, account.getPasswordHash())) {
            throw new PortalException(ErrorCode.ERR_UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다");
        }
        String token = jwtTokenProvider.generateToken(account.getId(), account.getRole(), account.getEmployeeId());
        return new LoginResponse(token, account.isMustChangePassword());
    }
}
