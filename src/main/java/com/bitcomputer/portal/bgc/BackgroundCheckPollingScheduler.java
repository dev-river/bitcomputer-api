package com.bitcomputer.portal.bgc;

import com.bitcomputer.portal.bgc.dto.BgcResultResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Component
public class BackgroundCheckPollingScheduler {

    private final BackgroundCheckMapper backgroundCheckMapper;
    private final BackgroundCheckApiClient apiClient;
    private final int maxRetryCount;
    private final boolean pollingEnabled;

    public BackgroundCheckPollingScheduler(BackgroundCheckMapper backgroundCheckMapper, BackgroundCheckApiClient apiClient,
                                            @Value("${bgc.max-poll-retry-count}") int maxRetryCount,
                                            @Value("${bgc.polling-enabled:true}") boolean pollingEnabled) {
        this.backgroundCheckMapper = backgroundCheckMapper;
        this.apiClient = apiClient;
        this.maxRetryCount = maxRetryCount;
        this.pollingEnabled = pollingEnabled;
    }

    @Scheduled(fixedDelayString = "${bgc.poll-interval-ms}")
    public void pollPendingChecks() {
        if (!pollingEnabled) {
            return;
        }
        for (BackgroundCheck check : backgroundCheckMapper.findAllPending()) {
            pollOne(check);
        }
    }

    public void pollOne(BackgroundCheck check) {
        if (check.getExternalCheckId() == null) {
            backgroundCheckMapper.updateStatusToError(check.getId(), "external_check_id missing for a pending row");
            return;
        }

        try {
            BgcResultResponse result = apiClient.get(check.getExternalCheckId());
            String status = result.getStatus().toUpperCase();
            if ("PENDING".equals(status)) {
                return;
            }
            backgroundCheckMapper.updateAfterPollSuccess(check.getId(), status,
                result.getCriminalRecord() == null ? null : result.getCriminalRecord().toString(),
                result.getEducationVerified() == null ? null : result.getEducationVerified().toString(),
                result.getEmploymentVerified() == null ? null : result.getEmploymentVerified().toString(),
                result.getCreditScore(),
                parseCompletedAt(result.getCompletedAt()));
        } catch (BackgroundCheckApiException e) {
            if (check.getRetryCount() + 1 >= maxRetryCount) {
                backgroundCheckMapper.updateStatusToError(check.getId(), e.getMessage());
            } else {
                backgroundCheckMapper.updateAfterPollError(check.getId(), e.getMessage(), LocalDateTime.now());
            }
        }
    }

    private LocalDateTime parseCompletedAt(String iso) {
        return iso == null ? LocalDateTime.now() : OffsetDateTime.parse(iso).toLocalDateTime();
    }
}
