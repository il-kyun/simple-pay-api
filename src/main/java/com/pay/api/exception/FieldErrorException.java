package com.pay.api.exception;

import org.springframework.validation.BindingResult;

import javax.validation.constraints.NotNull;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class FieldErrorException extends RuntimeException {

    public FieldErrorException(@NotNull BindingResult error) {
        super(error.getFieldErrors()
                .stream()
                .map((e -> {
                    StringJoiner stringJoiner = new StringJoiner(",");
                    return stringJoiner
                            .add(e.getField())
                            .add(String.valueOf(e.getRejectedValue()))
                            .add(e.getDefaultMessage()).toString();
                }))
                .collect(Collectors.joining(System.lineSeparator())));
    }

    public FieldErrorException(String message) {
        super(message);
    }
}