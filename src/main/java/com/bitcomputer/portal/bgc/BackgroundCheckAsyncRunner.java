package com.bitcomputer.portal.bgc;

import com.bitcomputer.portal.bgc.dto.BgcCreateRequest;
import com.bitcomputer.portal.bgc.dto.BgcCreateResponse;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Runs the actual call to the external Background Check API off the caller's thread.
 * {@code @Async} only takes effect on a call that goes through the Spring proxy — i.e. a call from
 * a different bean — so this lives in its own component rather than as a method on
 * {@link BackgroundCheckService} (a self-invocation there would silently run synchronously).
 */
@Component
public class BackgroundCheckAsyncRunner {

    private final BackgroundCheckMapper backgroundCheckMapper;
    private final BackgroundCheckApiClient apiClient;

    public BackgroundCheckAsyncRunner(BackgroundCheckMapper backgroundCheckMapper, BackgroundCheckApiClient apiClient) {
        this.backgroundCheckMapper = backgroundCheckMapper;
        this.apiClient = apiClient;
    }

    @Async
    public void submit(int checkId, String empNo, NameSplitter.SplitName name, String dateOfBirth) {
        try {
            BgcCreateResponse response = apiClient.create(new BgcCreateRequest(empNo, name.firstName(), name.lastName(), dateOfBirth));
            backgroundCheckMapper.updateAfterCreateSuccess(checkId, response.getCheckId(), response.getStatus().toUpperCase());
        } catch (BackgroundCheckApiException e) {
            backgroundCheckMapper.updateStatusToError(checkId, e.getMessage());
        }
    }
}
