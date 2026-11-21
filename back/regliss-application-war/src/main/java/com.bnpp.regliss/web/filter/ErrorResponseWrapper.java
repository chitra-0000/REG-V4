package com.bnpp.regliss.web.filter;

public class ErrorResponseWrapper {

    private ErrorResponse error;

    public ErrorResponseWrapper(ErrorResponse error) {
        super();
        this.error = error;
    }

    public ErrorResponse getError() {
        return error;
    }

    public void setError(ErrorResponse error) {
        this.error = error;
    }

}

