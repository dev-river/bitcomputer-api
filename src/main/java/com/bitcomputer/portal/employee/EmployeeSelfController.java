package com.bitcomputer.portal.employee;

import com.bitcomputer.portal.common.ApiResponse;
import com.bitcomputer.portal.employee.dto.EmployeeMeResponse;
import com.bitcomputer.portal.employee.dto.UpdateMeRequest;
import com.bitcomputer.portal.security.AuthenticatedAccount;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/me")
@PreAuthorize("hasRole('EMPLOYEE')")
public class EmployeeSelfController {

    private final EmployeeService employeeService;

    public EmployeeSelfController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public ApiResponse<EmployeeMeResponse> getMe(@AuthenticationPrincipal AuthenticatedAccount principal) {
        return ApiResponse.success(EmployeeMeResponse.from(employeeService.getMyInfo(principal.employeeId())));
    }

    @PatchMapping
    public ApiResponse<Void> updateMe(@AuthenticationPrincipal AuthenticatedAccount principal,
                                       @RequestBody UpdateMeRequest request) {
        employeeService.updateMyInfo(principal.employeeId(), request.getPhone(), request.getEmail(), request.getAddress());
        return ApiResponse.success(null);
    }
}
