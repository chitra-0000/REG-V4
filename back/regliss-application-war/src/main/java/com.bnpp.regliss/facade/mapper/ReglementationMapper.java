package com.bnpp.regliss.facade.mapper;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.ReglissException.ErrorCode;
import com.bnpp.regliss.entity.Reglementation;
import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.entity.record.Program;
import com.bnpp.regliss.entity.referential.record.SanctionType;
import com.bnpp.regliss.facade.dto.version.BaseReglementationDto;
import com.bnpp.regliss.facade.dto.version.ReglementationDto;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.repository.ProgramRepository;
import com.bnpp.regliss.repository.SanctionTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@ReglissWebProfile
public class ReglementationMapper {

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private SanctionTypeRepository sanctionTypeRepository;

    public Reglementation toEntity(Reglementation entity, ReglementationDto dto) {
        entity.setComment(dto.comment);
        entity.setDeleted(dto.id != null && dto.isDeleted);
        entity.setOfficialDate(dto.officialDate);
        if (dto.programId != null) {
            entity.setProgram(programRepository.getExactlyOne(dto.programId));
        } else {
            entity.setAllPrograms();
        }
        entity.setReceptionDate(dto.receptionDate);
        entity.setReference(dto.reference);
        entity.setSanctionType(sanctionTypeRepository.getExactlyOne(dto.sanctionType));
        entity.setType(dto.type);
        entity.setTitle(dto.title);
        entity.setUrl(dto.url);

        return entity;
    }

    public ReglementationDto toDto(Reglementation entity) {
        ReglementationDto dto = (ReglementationDto) toBaseDto(entity, new ReglementationDto());
        dto.comment = entity.getComment();
        dto.fileName = entity.getFileName();
        dto.isDeleted = entity.isDeleted();
        dto.programId = entity.getProgramOpt().map(Program::getId).orElse(null);
        dto.receptionDate = entity.getReceptionDate();
        dto.sanctionType = entity.getSanctionTypeOpt().map(SanctionType::getId).orElse(null);
        dto.url = entity.getUrl();
        dto.listId = entity.getListOpt().map(ReglissList::getId).orElse(null);

        return dto;
    }


    public List<BaseReglementationDto> toBaseDtos(List<Reglementation> entities) {
        List<BaseReglementationDto> dtos = new ArrayList<>();
        for (Reglementation entity : entities) {
            BaseReglementationDto dto = new BaseReglementationDto();
            dtos.add(toBaseDto(entity, dto));
        }
        return dtos;
    }

    private BaseReglementationDto toBaseDto(Reglementation entity, BaseReglementationDto dto) {
        dto.id = entity.getId();
        dto.officialDate = entity.getOfficialDate();
        dto.reference = entity.getReference();
        dto.title = entity.getTitle();
        dto.type = entity.getType();
        return dto;
    }

    public void addFileToEntity(Reglementation entity, MultipartFile file) {
        try {
            entity.setFileData(file.getBytes());
        } catch (IOException e) {
            throw new ReglissException("Exception while processing file", e, ErrorCode.FILE_PROCESS_ERROR);
        }
        entity.setFileContentType(file.getContentType());
        entity.setFileName(file.getOriginalFilename());
    }

}
