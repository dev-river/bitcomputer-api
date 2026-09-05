package com.bitcomputer.portal.employee;

import java.time.LocalDateTime;

public class EmployeeChangeLog {
    private Integer id;
    private Integer employeeId;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private LocalDateTime changedAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }
    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}
