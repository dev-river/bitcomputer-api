package com.bitcomputer.portal.bgc;

import com.bitcomputer.portal.bgc.dto.BgcCreateRequest;
import com.bitcomputer.portal.bgc.dto.BgcCreateResponse;
import com.bitcomputer.portal.common.ErrorCode;
import com.bitcomputer.portal.common.PortalException;
import com.bitcomputer.portal.employee.Employee;
import com.bitcomputer.portal.employee.EmployeeService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class BackgroundCheckService {

    private final EmployeeService employeeService;
    private final BackgroundCheckMapper backgroundCheckMapper;
    private final BackgroundCheckApiClient apiClient;

    public BackgroundCheckService(EmployeeService employeeService, BackgroundCheckMapper backgroundCheckMapper,
                                   BackgroundCheckApiClient apiClient) {
        this.employeeService = employeeService;
        this.backgroundCheckMapper = backgroundCheckMapper;
        this.apiClient = apiClient;
    }

    public void triggerForEmployee(int employeeId, int triggeredByAccountId) {
        Employee employee = employeeService.getById(employeeId);
        NameSplitter.SplitName name = NameSplitter.split(employee.getName());

        BackgroundCheck check = new BackgroundCheck();
        check.setEmployeeId(employeeId);
        check.setRequestedAt(LocalDateTime.now());
        check.setTriggeredByAccountId(triggeredByAccountId);

        try {
            BgcCreateResponse response = apiClient.create(new BgcCreateRequest(
                employee.getEmpNo(), name.firstName(), name.lastName(), employee.getDateOfBirth()));
            check.setExternalCheckId(response.getCheckId());
            check.setStatus(response.getStatus().toUpperCase());
        } catch (BackgroundCheckApiException e) {
            check.setStatus("ERROR");
            check.setErrorMessage(e.getMessage());
        }

        backgroundCheckMapper.insert(check);
    }

    public void triggerManualRerun(int employeeId, int triggeredByAccountId) {
        if (backgroundCheckMapper.existsPendingByEmployeeId(employeeId)) {
            throw new PortalException(ErrorCode.ERR_DUPLICATE, "이미 진행중인 검사가 있습니다");
        }
        triggerForEmployee(employeeId, triggeredByAccountId);
    }
}
