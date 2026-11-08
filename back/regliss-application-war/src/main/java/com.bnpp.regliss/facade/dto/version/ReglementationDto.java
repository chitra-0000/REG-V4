package com.bnpp.regliss.facade.dto.version;

import java.time.LocalDate;

public class ReglementationDto extends BaseReglementationDto {

    public LocalDate receptionDate;
    public String url;
    public Long sanctionType;
    public Long programId;
    public String comment;
    public boolean isDeleted;
    public String fileContentType;
    public String fileName;
    public Long listId;

    public ReglementationDto(long id) { super(id); }

    public ReglementationDto() { super(); }

}

