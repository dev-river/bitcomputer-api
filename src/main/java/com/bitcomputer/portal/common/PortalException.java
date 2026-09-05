package com.bitcomputer.portal.common;

public class PortalException extends RuntimeException {
    private final ErrorCode errorCode;

    public PortalException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() { return errorCode; }
}
