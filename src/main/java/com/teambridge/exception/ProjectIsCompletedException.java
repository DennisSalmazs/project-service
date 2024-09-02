package com.teambridge.exception;

public class ProjectIsCompletedException extends RuntimeException {

    public ProjectIsCompletedException(String message) {
        super(message);
    }

}
