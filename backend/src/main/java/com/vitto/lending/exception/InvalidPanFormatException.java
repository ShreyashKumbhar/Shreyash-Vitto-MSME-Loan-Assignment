package com.vitto.lending.exception;

public class InvalidPanFormatException extends RuntimeException {
    public InvalidPanFormatException(String message) {
        super(message);
    }
}
