package com.bitcomputer.portal.admin.dto;

import com.bitcomputer.portal.employee.Employee;

public class EmployeeListItemResponse {
    private Integer id;
    private String empNo;
    private String name;
    private String department;
    private String position;
    private String status;
    private boolean hasAccount;

    public static EmployeeListItemResponse from(Employee e, boolean hasAccount) {
        EmployeeListItemResponse dto = new EmployeeListItemResponse();
        dto.id = e.getId();
        dto.empNo = e.getEmpNo();
        dto.name = e.getName();
        dto.department = e.getDepartment();
        dto.position = e.getPosition();
        dto.status = e.getStatus();
        dto.hasAccount = hasAccount;
        return dto;
    }

    public Integer getId() { return id; }
    public String getEmpNo() { return empNo; }
    public String getName() { return name; }
    public String getDepartment() { return department; }
    public String getPosition() { return position; }
    public String getStatus() { return status; }
    public boolean isHasAccount() { return hasAccount; }
}
