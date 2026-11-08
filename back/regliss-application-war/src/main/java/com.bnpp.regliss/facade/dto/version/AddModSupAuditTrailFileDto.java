package com.bnpp.regliss.facade.dto.version;

import com.bnpp.regliss.entity.AddModSupAuditTrailFile;

public class AddModSupAuditTrailFileDto {
    public long id;
    public String fileName;

    public AddModSupAuditTrailFileDto(AddModSupAuditTrailFile entity) {
        id = entity.getId();
        fileName = entity.getFileName();
    }
}

