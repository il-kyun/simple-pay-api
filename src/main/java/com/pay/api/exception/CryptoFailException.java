package com.pay.api.exception;

public class CryptoFailException extends RuntimeException {

    public CryptoFailException(String message) {
        super(message);
    }
}