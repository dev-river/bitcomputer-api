package com.bitcomputer.portal.employee;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface EmployeeChangeLogMapper {
    int insert(EmployeeChangeLog log);
    List<EmployeeChangeLog> findByEmployeeId(int employeeId);
}
