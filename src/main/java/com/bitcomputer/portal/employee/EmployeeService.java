package com.bitcomputer.portal.employee;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional
    public void updateMyInfo(int employeeId, String phone, String email, String address) {
        Employee before = employeeMapper.findById(employeeId);
        // A field omitted from the request body deserializes to null on UpdateMeRequest — treat
        // that as "leave unchanged", not "clear it". Without this fallback, a partial PATCH body
        // silently nulls out the other two columns and logs a bogus change-log entry recording a
        // change the employee never made. Found during Task 7's review, not in the original brief.
        String newPhone = phone != null ? phone : before.getPhone();
        String newEmail = email != null ? email : before.getEmail();
        String newAddress = address != null ? address : before.getAddress();

        employeeMapper.updateContactInfo(employeeId, newPhone, newEmail, newAddress);
        logIfChanged(employeeId, "phone", before.getPhone(), newPhone);
        logIfChanged(employeeId, "email", before.getEmail(), newEmail);
        logIfChanged(employeeId, "address", before.getAddress(), newAddress);
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

    public java.util.List<Employee> search(EmployeeSearchCriteria criteria) {
        return employeeMapper.search(criteria);
    }

    public Employee getById(int id) {
        Employee employee = employeeMapper.findById(id);
        if (employee == null) {
            throw new com.bitcomputer.portal.common.PortalException(
                com.bitcomputer.portal.common.ErrorCode.ERR_NOT_FOUND, "직원을 찾을 수 없습니다: " + id);
        }
        return employee;
    }

    public void terminate(int id) {
        getById(id);
        employeeMapper.updateStatus(id, "TERMINATED", java.time.LocalDateTime.now());
    }

    @Transactional
    public Employee create(String name, String department, String position, java.time.LocalDate hireDate,
                            String email, String phone, String address, String dateOfBirth) {
        Integer maxSuffix = employeeMapper.findMaxEmpNoSuffix();
        String empNo = String.format("EMP-%03d", (maxSuffix != null ? maxSuffix : 0) + 1);

        Employee employee = new Employee();
        employee.setEmpNo(empNo);
        employee.setName(name);
        employee.setDepartment(department);
        employee.setPosition(position);
        employee.setHireDate(hireDate);
        employee.setEmail(email);
        employee.setPhone(phone);
        employee.setAddress(address);
        employee.setDateOfBirth(dateOfBirth);
        employee.setStatus("ACTIVE");
        employeeMapper.insert(employee);
        return employee;
    }
}
