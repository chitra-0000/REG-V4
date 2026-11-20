package com.bnpp.regliss.scheduler;

import lombok.Data;

@Data
public class CftLogEntry {
    private String timeId;
    private boolean ok;
    private int returnCode;
    private String message;
    private String pathToTransferredFile;
    private String originalLogLine;
    private String idF;
    private String fileNameWithExtension;
}

