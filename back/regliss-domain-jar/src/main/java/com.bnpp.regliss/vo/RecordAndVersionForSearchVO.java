package com.bnpp.regliss.vo;

import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.record.Record;
import com.bnpp.regliss.entity.referential.Confidentiality;
import lombok.Data;

@Data
public class RecordAndVersionForSearchVO {

    private Record record;
    private Long fromVersionNumber;
    private Long fromVersionId;
    private Long toVersionNumber;
    private Long toVersionId;
    private Long deletedInVersionId;
    private boolean isActive;
    private Confidentiality listConfidentiality;
    private String listShortName;


    public boolean recordIsDeletedInVersion(Version lastVersion) {
        return deletedInVersionId!=null && deletedInVersionId.equals(lastVersion.getId());
    }

}
