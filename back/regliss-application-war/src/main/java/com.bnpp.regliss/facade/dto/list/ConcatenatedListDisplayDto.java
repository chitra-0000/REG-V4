package com.bnpp.regliss.facade.dto.list;

import java.time.LocalDateTime;

public class ConcatenatedListDisplayDto {
    public Long id;
    public String label;
    public LocalDateTime publicationDate;
    public boolean active;

    public ConcatenatedListDisplayDto(Long id, String label, LocalDateTime publicationDate, boolean active){
        this.id = id;
        this.label = label;
        this.publicationDate = publicationDate;
        this.active = active;
    }
}

