package com.hongyuwu.careerchronicle.data;

public final class RegistryValidationException extends RuntimeException {
    public RegistryValidationException(String message) {
        super(message);
    }

    public RegistryValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
