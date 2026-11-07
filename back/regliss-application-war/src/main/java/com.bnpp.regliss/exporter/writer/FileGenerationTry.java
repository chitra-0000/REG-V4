package com.bnpp.regliss.exporter.writer;

import lombok.Getter;

import java.io.File;

public class FileGenerationTry {
    @Getter
    private File file;
    @Getter
    private Exception exception;

    public FileGenerationTry(File file) {
        this.file = file;

    }

    public FileGenerationTry(Exception exception) { this.exception = exception; }

    private FileGenerationTry() {
    }

    public boolean isOk() { return exception == null; }

    public boolean hasErrors() { return !isOk(); }





}
