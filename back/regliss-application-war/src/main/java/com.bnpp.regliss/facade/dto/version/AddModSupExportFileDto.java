package com.bnpp.regliss.facade.dto.version;

import com.bnpp.regliss.entity.AddModSupExportFile;

public class AddModSupExportFileDto{

    public long id;
    public String fileName;

    public AddModSupExportFileDto(AddModSupExportFile entity) {
        id = entity.getId();
        fileName = entity.getFileName();
    }
}

