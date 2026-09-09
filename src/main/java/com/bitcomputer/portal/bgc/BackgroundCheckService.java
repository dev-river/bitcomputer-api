package com.bitcomputer.portal.bgc;

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
    private final BackgroundCheckAsyncRunner asyncRunner;

    public BackgroundCheckService(EmployeeService employeeService, BackgroundCheckMapper backgroundCheckMapper,
                                   BackgroundCheckAsyncRunner asyncRunner) {
        this.employeeService = employeeService;
        this.backgroundCheckMapper = backgroundCheckMapper;
        this.asyncRunner = asyncRunner;
    }

    /**
     * Inserts the audit-trail row and returns immediately — the call to the external API (which
     * MEASUREMENTS.md §1-1 shows can legitimately take up to ~30s per attempt) runs on a separate
     * thread via {@link BackgroundCheckAsyncRunner}, so this no longer blocks the account-creation
     * or manual-rerun request while the external API is slow (MEASUREMENTS.md §4-1).
     */
    public void triggerForEmployee(int employeeId, int triggeredByAccountId) {
        Employee employee = employeeService.getById(employeeId);
        // Split before inserting: an invalid name (too short to split) should fail fast with no
        // row created, same as before this change — not surface as a silent ERROR row.
        NameSplitter.SplitName name = NameSplitter.split(employee.getName());

        BackgroundCheck check = new BackgroundCheck();
        check.setEmployeeId(employeeId);
        check.setStatus("PENDING");
        check.setRequestedAt(LocalDateTime.now());
        check.setTriggeredByAccountId(triggeredByAccountId);
        backgroundCheckMapper.insert(check);

        asyncRunner.submit(check.getId(), employee.getEmpNo(), name, employee.getDateOfBirth());
    }

    public void triggerManualRerun(int employeeId, int triggeredByAccountId) {
        if (backgroundCheckMapper.existsPendingByEmployeeId(employeeId)) {
            throw new PortalException(ErrorCode.ERR_DUPLICATE, "이미 진행중인 검사가 있습니다");
        }
        triggerForEmployee(employeeId, triggeredByAccountId);
    }
}
