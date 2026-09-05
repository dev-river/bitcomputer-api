package com.bitcomputer.portal.auth;

import com.bitcomputer.portal.auth.dto.LoginRequest;
import com.bitcomputer.portal.auth.dto.LoginResponse;
import com.bitcomputer.portal.common.ApiResponse;
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
}
