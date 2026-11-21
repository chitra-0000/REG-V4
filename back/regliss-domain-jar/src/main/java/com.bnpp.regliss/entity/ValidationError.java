package com.bnpp.regliss.entity;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.io.Serializable;
import java.util.Objects;
import java.util.stream.Stream;

import com.bnpp.regliss.ReglissException.ErrorCode;

/serial/ /* TODO: illegible, verify - appears to be a collapsed/folded comment in the IDE */
public class ValidationError implements Serializable {

    private ErrorCode errorCode;
    private String[] parameters;

    public ValidationError(ErrorCode errorCode, Object... parameters) {
        this.errorCode = errorCode;
        this.parameters = Stream.of(parameters).map(Objects::toString).collect(toList()).toArray(new String[0]);
    }

    public ValidationError() {

    }

    public ErrorCode getErrorCode() { return errorCode; }

    public String[] getParameters() { return parameters; }

    @Override
    public String toString() { return errorCode+Stream.of(parameters).collect(joining(",", "(", ")")); }
}
