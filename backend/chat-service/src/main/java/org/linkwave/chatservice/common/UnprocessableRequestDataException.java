package org.linkwave.chatservice.common;

/** Dữ liệu đúng định dạng nhưng vi phạm quy tắc nghiệp vụ (HTTP 422). */
public class UnprocessableRequestDataException extends RuntimeException {

    public UnprocessableRequestDataException(String message) {
        super(message);
    }

}
