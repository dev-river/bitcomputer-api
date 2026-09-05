package com.bitcomputer.portal.admin;

import com.bitcomputer.portal.admin.dto.BackgroundCheckHistoryResponse;
import com.bitcomputer.portal.bgc.BackgroundCheckMapper;
import com.bitcomputer.portal.bgc.BackgroundCheckService;
import com.bitcomputer.portal.common.ApiResponse;
import com.bitcomputer.portal.security.AuthenticatedAccount;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/employees/{employeeId}/background-checks")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBackgroundCheckController {

    private final BackgroundCheckService backgroundCheckService;
    private final BackgroundCheckMapper backgroundCheckMapper;

    public AdminBackgroundCheckController(BackgroundCheckService backgroundCheckService, BackgroundCheckMapper backgroundCheckMapper) {
        this.backgroundCheckService = backgroundCheckService;
        this.backgroundCheckMapper = backgroundCheckMapper;
    }

    @PostMapping
    public ApiResponse<Void> rerun(@PathVariable int employeeId, @AuthenticationPrincipal AuthenticatedAccount principal) {
        backgroundCheckService.triggerManualRerun(employeeId, principal.accountId());
        return ApiResponse.success(null);
    }

    @GetMapping
    public ApiResponse<List<BackgroundCheckHistoryResponse>> history(@PathVariable int employeeId) {
        List<BackgroundCheckHistoryResponse> result = backgroundCheckMapper.findByEmployeeId(employeeId).stream()
            .map(BackgroundCheckHistoryResponse::from).toList();
        return ApiResponse.success(result);
    }
}
