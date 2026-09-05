package com.bitcomputer.portal.bgc;

import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BackgroundCheckMapper {
    int insert(BackgroundCheck check);
    BackgroundCheck findById(int id);
    List<BackgroundCheck> findByEmployeeId(int employeeId);
    boolean existsPendingByEmployeeId(int employeeId);
    List<BackgroundCheck> findAllPending();
    int updateAfterPollSuccess(int id, String status, String criminalRecord, String educationVerified,
                               String employmentVerified, String creditScore, LocalDateTime completedAt);
    int updateAfterPollError(int id, String errorMessage, LocalDateTime lastErrorAt);
    int updateStatusToError(int id, String errorMessage);
    List<BackgroundCheck> findMaskCandidates(LocalDateTime cutoff);
    int maskSensitiveFields(int id, LocalDateTime maskedAt);
}
