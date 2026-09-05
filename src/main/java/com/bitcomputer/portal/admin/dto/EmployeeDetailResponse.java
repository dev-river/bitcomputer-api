package com.bitcomputer.portal.admin.dto;

import com.bitcomputer.portal.employee.Employee;
import com.bitcomputer.portal.employee.EmployeeChangeLog;

import java.util.List;

public class EmployeeDetailResponse {
    private Integer id;
    private String empNo;
    private String name;
    private String department;
    private String position;
    private String email;
    private String dateOfBirth;
    private String phone;
    private String address;
    private String status;
    private List<EmployeeChangeLog> changeLogs;

    public static EmployeeDetailResponse from(Employee e, List<EmployeeChangeLog> changeLogs) {
        EmployeeDetailResponse dto = new EmployeeDetailResponse();
        dto.id = e.getId();
        dto.empNo = e.getEmpNo();
        dto.name = e.getName();
        dto.department = e.getDepartment();
        dto.position = e.getPosition();
        dto.email = e.getEmail();
        dto.dateOfBirth = e.getDateOfBirth();
        dto.phone = e.getPhone();
        dto.address = e.getAddress();
        dto.status = e.getStatus();
        dto.changeLogs = changeLogs;
        return dto;
    }

    public Integer getId() { return id; }
    public String getEmpNo() { return empNo; }
    public String getName() { return name; }
    public String getDepartment() { return department; }
    public String getPosition() { return position; }
    public String getEmail() { return email; }
    public String getDateOfBirth() { return dateOfBirth; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getStatus() { return status; }
    public List<EmployeeChangeLog> getChangeLogs() { return changeLogs; }
}
