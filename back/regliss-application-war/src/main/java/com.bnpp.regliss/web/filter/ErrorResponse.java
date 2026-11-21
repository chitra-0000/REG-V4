package com.bnpp.regliss.web.filter;

public class ErrorResponse {

    private final int status;
    private String requestId;
    private String userMessage;
    private String errorCode;

    public ErrorResponse(int status) {
        super();
        this.status = status;
    }



    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }



    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public int getStatus() {
        return status;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }


}

