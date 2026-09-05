package com.bitcomputer.portal.admin;

import com.bitcomputer.portal.account.AccountMapper;
import com.bitcomputer.portal.admin.dto.EmployeeDetailResponse;
import com.bitcomputer.portal.admin.dto.EmployeeListItemResponse;
import com.bitcomputer.portal.common.ApiResponse;
import com.bitcomputer.portal.employee.Employee;
import com.bitcomputer.portal.employee.EmployeeChangeLogMapper;
import com.bitcomputer.portal.employee.EmployeeSearchCriteria;
import com.bitcomputer.portal.employee.EmployeeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/employees")
@PreAuthorize("hasRole('ADMIN')")
public class AdminEmployeeController {

    private final EmployeeService employeeService;
    private final AccountMapper accountMapper;
    private final EmployeeChangeLogMapper changeLogMapper;

    public AdminEmployeeController(EmployeeService employeeService, AccountMapper accountMapper,
                                    EmployeeChangeLogMapper changeLogMapper) {
        this.employeeService = employeeService;
        this.accountMapper = accountMapper;
        this.changeLogMapper = changeLogMapper;
    }

    @GetMapping
    public ApiResponse<List<EmployeeListItemResponse>> list(EmployeeSearchCriteria criteria) {
        List<EmployeeListItemResponse> result = employeeService.search(criteria).stream()
            .map(e -> EmployeeListItemResponse.from(e, accountMapper.findByEmployeeId(e.getId()) != null))
            .toList();
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<EmployeeDetailResponse> detail(@PathVariable int id) {
        Employee employee = employeeService.getById(id);
        return ApiResponse.success(EmployeeDetailResponse.from(employee, changeLogMapper.findByEmployeeId(id)));
    }

    @PostMapping("/{id}/terminate")
    public ApiResponse<Void> terminate(@PathVariable int id) {
        employeeService.terminate(id);
        return ApiResponse.success(null);
    }
}
