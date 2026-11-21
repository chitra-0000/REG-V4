package com.bnpp.regliss.six.importer.exception;

public class SixImportException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SixImportException(String message) {
        super(message);
    }

    public SixImportException(String message, Throwable cause) {
        super(message, cause);
    }
}


