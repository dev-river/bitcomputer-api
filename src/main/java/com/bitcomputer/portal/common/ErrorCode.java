package com.bitcomputer.portal.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    ERR_UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    ERR_FORBIDDEN(HttpStatus.FORBIDDEN),
    ERR_NOT_FOUND(HttpStatus.NOT_FOUND),
    ERR_DUPLICATE(HttpStatus.CONFLICT),
    ERR_VALIDATION(HttpStatus.BAD_REQUEST),
    ERR_UPSTREAM_FAILURE(HttpStatus.BAD_GATEWAY);

    private final HttpStatus httpStatus;

    ErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() { return httpStatus; }
}
