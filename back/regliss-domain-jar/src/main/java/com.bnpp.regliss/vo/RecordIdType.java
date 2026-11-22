package com.bnpp.regliss.vo;

import com.bnpp.regliss.entity.referential.record.RecordType;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;


@EqualsAndHashCode(of = "id")
public class RecordIdType {

    @Getter
    private final Long id;
    @Getter
    private final RecordType type;
    @Getter
    private final long createdInVersion;
    @Getter
    private final long clonedInversion;

    public RecordIdType(Long id){
        this.type=null;
        createdInVersion = -1L;
        clonedInversion = -1L;
        this.id = id;
    }

    public RecordIdType(Long id, RecordType type, Long createdInVersion, Long clonedInversion){
        this.type=type;
        this.createdInVersion = createdInVersion==null ? -1L: createdInVersion;
        this.clonedInversion = clonedInversion == null? -1L: clonedInversion;
        this.id = id;
    }

}
