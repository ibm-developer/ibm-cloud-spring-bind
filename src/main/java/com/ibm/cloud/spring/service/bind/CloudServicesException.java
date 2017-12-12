package com.ibm.cloud.spring.service.bind;

public class CloudServicesException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CloudServicesException() {
    }

    public CloudServicesException(String message) {
        super(message);
    }

    public CloudServicesException(Throwable cause) {
        super(cause);
    }

    public CloudServicesException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloudServicesException(String message,
                                  Throwable cause,
                                  boolean enableSuppression,
                                  boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
