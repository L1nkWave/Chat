package org.linkwave.chatservice.common;

public class PrivacyViolationException extends RuntimeException {

    public PrivacyViolationException() {
        this("Do not have permissions to access the resource");
    }

    public PrivacyViolationException(String message) {
        super(message);
    }

}
