package com.bnpp.regliss.facade.dto.version;

import com.bnpp.regliss.entity.ExportFile.GenerationStatus;

public class VersionSynthesisFileDto {
    public Long id;
    public boolean regenerate;
    public GenerationStatus status;
}

