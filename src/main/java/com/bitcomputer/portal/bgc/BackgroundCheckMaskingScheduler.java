package com.bitcomputer.portal.bgc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class BackgroundCheckMaskingScheduler {

    private final BackgroundCheckMapper backgroundCheckMapper;
    private final int retentionDays;

    public BackgroundCheckMaskingScheduler(BackgroundCheckMapper backgroundCheckMapper,
                                            @Value("${bgc.masking-retention-days}") int retentionDays) {
        this.backgroundCheckMapper = backgroundCheckMapper;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "${bgc.masking-cron}")
    public void maskExpiredResults() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        for (BackgroundCheck check : backgroundCheckMapper.findMaskCandidates(cutoff)) {
            backgroundCheckMapper.maskSensitiveFields(check.getId(), LocalDateTime.now());
        }
    }
}
