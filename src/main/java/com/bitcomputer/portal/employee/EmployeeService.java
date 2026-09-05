package com.bitcomputer.portal.employee;

import org.springframework.stereotype.Service;
import java.util.Objects;

@Service
public class EmployeeService {

    private final EmployeeMapper employeeMapper;
    private final EmployeeChangeLogMapper changeLogMapper;

    public EmployeeService(EmployeeMapper employeeMapper, EmployeeChangeLogMapper changeLogMapper) {
        this.employeeMapper = employeeMapper;
        this.changeLogMapper = changeLogMapper;
    }

    public Employee getMyInfo(int employeeId) {
        return employeeMapper.findById(employeeId);
    }

    public void updateMyInfo(int employeeId, String phone, String email, String address) {
        Employee before = employeeMapper.findById(employeeId);
        employeeMapper.updateContactInfo(employeeId, phone, email, address);
        logIfChanged(employeeId, "phone", before.getPhone(), phone);
        logIfChanged(employeeId, "email", before.getEmail(), email);
        logIfChanged(employeeId, "address", before.getAddress(), address);
    }

    private void logIfChanged(int employeeId, String field, String oldValue, String newValue) {
        if (Objects.equals(oldValue, newValue)) {
            return;
        }
        EmployeeChangeLog log = new EmployeeChangeLog();
        log.setEmployeeId(employeeId);
        log.setFieldName(field);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        changeLogMapper.insert(log);
    }
}
