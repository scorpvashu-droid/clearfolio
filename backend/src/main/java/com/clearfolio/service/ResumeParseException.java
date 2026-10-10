package com.clearfolio.service;

public class ResumeParseException extends RuntimeException {
    private final int status;

    public ResumeParseException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
