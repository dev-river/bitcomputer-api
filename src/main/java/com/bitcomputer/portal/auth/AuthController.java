package com.bitcomputer.portal.auth;

import com.bitcomputer.portal.auth.dto.ChangePasswordRequest;
import com.bitcomputer.portal.auth.dto.LoginRequest;
import com.bitcomputer.portal.auth.dto.LoginResponse;
import com.bitcomputer.portal.common.ApiResponse;
import com.bitcomputer.portal.security.AuthenticatedAccount;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request.getLoginId(), request.getPassword()));
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal AuthenticatedAccount principal,
                                             @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.accountId(), request.getCurrentPassword(), request.getNewPassword());
        return ApiResponse.success(null);
    }
}
