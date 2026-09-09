package com.bitcomputer.portal.employee;

import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface EmployeeMapper {
    Employee findById(int id);
    Employee findByEmpNo(String empNo);
    List<Employee> search(EmployeeSearchCriteria criteria);
    int updateContactInfo(int id, String phone, String email, String address);
    int updateStatus(int id, String status, LocalDateTime terminatedAt);
    int updateDateOfBirth(int id, String dateOfBirth);
    Integer findMaxEmpNoSuffix();
    int insert(Employee employee);
}
