package com.example.rag.common.exception;

public class LlmNotConfiguredException extends RuntimeException {

    public LlmNotConfiguredException(String message) {
        super(message);
    }
}
