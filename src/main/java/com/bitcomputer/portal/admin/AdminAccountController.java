package com.bitcomputer.portal.admin;

import com.bitcomputer.portal.admin.dto.CreateAccountRequest;
import com.bitcomputer.portal.admin.dto.CreateAccountResponse;
import com.bitcomputer.portal.common.ApiResponse;
import com.bitcomputer.portal.common.ErrorCode;
import com.bitcomputer.portal.common.PortalException;
import com.bitcomputer.portal.security.AuthenticatedAccount;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/accounts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    public AdminAccountController(AdminAccountService adminAccountService) {
        this.adminAccountService = adminAccountService;
    }

    @PostMapping
    public ApiResponse<CreateAccountResponse> create(@AuthenticationPrincipal AuthenticatedAccount principal,
                                                       @RequestBody CreateAccountRequest request) {
        if (request.getEmployeeId() == null) {
            // Without this check, autoboxing a null Integer into createAccountForEmployee(int, ...)
            // throws an NPE that GlobalExceptionHandler's catch-all turns into a generic 500 instead
            // of a clean 400 — found during Task 8's review.
            throw new PortalException(ErrorCode.ERR_VALIDATION, "employeeId는 필수입니다");
        }
        CreateAccountResponse response = adminAccountService.createAccountForEmployee(
            request.getEmployeeId(), principal.accountId());
        return ApiResponse.success(response);
    }
}
