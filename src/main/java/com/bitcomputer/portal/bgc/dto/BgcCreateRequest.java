package com.bitcomputer.portal.bgc.dto;

public class BgcCreateRequest {
    private String employeeId;
    private String firstName;
    private String lastName;
    private String dateOfBirth;

    public BgcCreateRequest(String employeeId, String firstName, String lastName, String dateOfBirth) {
        this.employeeId = employeeId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
    }

    public String getEmployeeId() { return employeeId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getDateOfBirth() { return dateOfBirth; }
}
