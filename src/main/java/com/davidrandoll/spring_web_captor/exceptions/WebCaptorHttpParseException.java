package com.davidrandoll.spring_web_captor.exceptions;

public class WebCaptorHttpParseException extends RuntimeException {
    public WebCaptorHttpParseException(Exception ex) {
        super("Failed to parse request body", ex);
    }
}
