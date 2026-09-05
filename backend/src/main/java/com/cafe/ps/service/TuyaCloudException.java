package com.cafe.ps.service;

/** Internal, sanitized representation of a Tuya Cloud failure. */
public class TuyaCloudException extends RuntimeException {
    private final Integer apiCode;
    private final boolean connectivityFailure;
    private final boolean authenticationFailure;

    public TuyaCloudException(
            String message,
            Integer apiCode,
            boolean connectivityFailure,
            boolean authenticationFailure
    ) {
        super(message);
        this.apiCode = apiCode;
        this.connectivityFailure = connectivityFailure;
        this.authenticationFailure = authenticationFailure;
    }

    public Integer getApiCode() {
        return apiCode;
    }

    public boolean isConnectivityFailure() {
        return connectivityFailure;
    }

    public boolean isAuthenticationFailure() {
        return authenticationFailure;
    }
}
