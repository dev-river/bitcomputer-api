package com.bitcomputer.portal.employee.dto;

public class EmployeeMeResponse {
    private String empNo;
    private String name;
    private String department;
    private String position;
    private String hireDate;
    private String email;
    private String phone;
    private String address;
    private String status;

    public static EmployeeMeResponse from(com.bitcomputer.portal.employee.Employee e) {
        EmployeeMeResponse dto = new EmployeeMeResponse();
        dto.empNo = e.getEmpNo();
        dto.name = e.getName();
        dto.department = e.getDepartment();
        dto.position = e.getPosition();
        dto.hireDate = e.getHireDate() == null ? null : e.getHireDate().toString();
        dto.email = e.getEmail();
        dto.phone = e.getPhone();
        dto.address = e.getAddress();
        dto.status = e.getStatus();
        return dto;
    }

    public String getEmpNo() { return empNo; }
    public String getName() { return name; }
    public String getDepartment() { return department; }
    public String getPosition() { return position; }
    public String getHireDate() { return hireDate; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getStatus() { return status; }
}
