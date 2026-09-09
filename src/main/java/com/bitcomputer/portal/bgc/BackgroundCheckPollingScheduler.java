package com.bitcomputer.portal.bgc;

import com.bitcomputer.portal.bgc.dto.BgcResultResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Component
public class BackgroundCheckPollingScheduler {

    private final BackgroundCheckMapper backgroundCheckMapper;
    private final BackgroundCheckApiClient apiClient;
    private final int maxRetryCount;
    private final long maxPendingDurationMs;
    private final boolean pollingEnabled;

    public BackgroundCheckPollingScheduler(BackgroundCheckMapper backgroundCheckMapper, BackgroundCheckApiClient apiClient,
                                            @Value("${bgc.max-poll-retry-count}") int maxRetryCount,
                                            @Value("${bgc.max-pending-duration-ms}") long maxPendingDurationMs,
                                            @Value("${bgc.polling-enabled:true}") boolean pollingEnabled) {
        this.backgroundCheckMapper = backgroundCheckMapper;
        this.apiClient = apiClient;
        this.maxRetryCount = maxRetryCount;
        this.maxPendingDurationMs = maxPendingDurationMs;
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

    /**
     * maxRetryCount only bounds consecutive API-call FAILURES — a check the API keeps answering
     * "200 pending" for never increments it and would poll forever (MEASUREMENTS.md §4-2: 12 real
     * checks were still pending after 4+ days, one historical case took 3.6 days to resolve). This
     * age check is the actual backstop: regardless of whether calls are succeeding, a check pending
     * longer than bgc.max-pending-duration-ms is given up on and left for a manual rerun.
     */
    public void pollOne(BackgroundCheck check) {
        if (isPendingTooLong(check)) {
            backgroundCheckMapper.updateStatusToError(check.getId(),
                "요청 후 " + (maxPendingDurationMs / 60_000) + "분 넘게 완료되지 않아 자동 종료됨 — 관리자가 재실행 가능");
            return;
        }

        if (check.getExternalCheckId() == null) {
            // BackgroundCheckAsyncRunner hasn't updated this row yet (its call to the external API
            // is still in flight) — not an error, just not ready to poll. Try again next tick.
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

    private boolean isPendingTooLong(BackgroundCheck check) {
        return Duration.between(check.getRequestedAt(), LocalDateTime.now()).toMillis() >= maxPendingDurationMs;
    }

    private LocalDateTime parseCompletedAt(String iso) {
        return iso == null ? LocalDateTime.now() : OffsetDateTime.parse(iso).toLocalDateTime();
    }
}
