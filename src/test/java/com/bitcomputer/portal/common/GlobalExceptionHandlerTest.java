package com.bitcomputer.portal.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void portalException_mapsToConfiguredHttpStatusAndErrorBody() {
        PortalException ex = new PortalException(ErrorCode.ERR_DUPLICATE, "이미 진행중인 검사가 있습니다");

        ResponseEntity<ApiResponse<Void>> response = handler.handlePortalException(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError().getCode()).isEqualTo("ERR_DUPLICATE");
        assertThat(response.getBody().getError().getMessage()).isEqualTo("이미 진행중인 검사가 있습니다");
    }
}
