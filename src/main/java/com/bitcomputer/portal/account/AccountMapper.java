package com.bitcomputer.portal.account;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccountMapper {
    Account findByLoginId(String loginId);
    Account findByEmployeeId(int employeeId);
    Account findById(int id);
    int insert(Account account);
    int updatePasswordAndClearMustChange(int id, String passwordHash);
}
