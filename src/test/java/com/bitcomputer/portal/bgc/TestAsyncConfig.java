package com.bitcomputer.portal.bgc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

// @Async methods (BackgroundCheckAsyncRunner.submit) would otherwise run on a background thread,
// making test assertions that check background_check state right after calling
// triggerForEmployee(...)/triggerManualRerun(...) racy. A single TaskExecutor bean named
// "taskExecutor" is what @EnableAsync picks up by default — this pins it to run inline on the
// calling thread only in tests (still driven by the same test-thread @Transactional, so it rolls
// back like every other test write).
@Configuration
@Profile("test")
public class TestAsyncConfig {

    @Bean
    public TaskExecutor taskExecutor() {
        return new SyncTaskExecutor();
    }
}
