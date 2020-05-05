package com.pay.api.exception;

public class IllegalStatusException extends RuntimeException {

    public IllegalStatusException(String message) {
        super(message);
    }
}