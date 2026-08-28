package com.owlvigil.errors;

public class OwlVigilException extends RuntimeException {
    public OwlVigilException(String message) { super(message); }
    public OwlVigilException(String message, Throwable cause) { super(message, cause); }
}
