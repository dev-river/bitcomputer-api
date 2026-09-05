package com.bitcomputer.portal.common;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PortalException.class)
    public ResponseEntity<ApiResponse<Void>> handlePortalException(PortalException ex) {
        return ResponseEntity
            .status(ex.getErrorCode().getHttpStatus())
            .body(ApiResponse.error(ex.getErrorCode().name(), ex.getMessage()));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        return ResponseEntity
            .status(ErrorCode.ERR_FORBIDDEN.getHttpStatus())
            .body(ApiResponse.error(ErrorCode.ERR_FORBIDDEN.name(), "접근 권한이 없습니다"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        return ResponseEntity
            .status(500)
            .body(ApiResponse.error("ERR_INTERNAL", "예기치 않은 오류가 발생했습니다"));
    }
}
