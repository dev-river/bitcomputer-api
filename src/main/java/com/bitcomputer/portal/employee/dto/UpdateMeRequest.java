package com.bitcomputer.portal.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class UpdateMeRequest {
    @Size(max = 20)
    private String phone;
    @Email
    @Size(max = 100)
    private String email;
    @Size(max = 200)
    private String address;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
