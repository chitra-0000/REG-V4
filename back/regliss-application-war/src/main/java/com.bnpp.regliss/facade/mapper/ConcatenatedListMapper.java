package com.bnpp.regliss.facade.mapper;

import com.bnpp.regliss.entity.ConcatenatedList;
import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.facade.dto.list.ConcatenatedListDisplayDto;
import com.bnpp.regliss.facade.dto.list.ConcatenatedListDto;
import com.bnpp.regliss.facade.dto.list.ConcatenatedListUniqueFieldsDto;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.repository.ConcatenatedListRepository;
import com.bnpp.regliss.vo.ConcatenatedListDisplayVO;
import com.bnpp.regliss.vo.ConcatenatedListUniqueFieldsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@ReglissWebProfile
public class ConcatenatedListMapper {

    @Autowired
    private ConcatenatedListRepository concatenatedlistRepo;
    @Autowired
    private ReglissListMapper reglissListMapper;

    public ConcatenatedListDto toDto(ConcatenatedList clist){
        ConcatenatedListDto dto = new ConcatenatedListDto();
        dto.id=clist.getId();
        dto.label=clist.getLabel();
        dto.publicationDate=clist.getPublicationDate();
        dto.deleted=clist.isDeleted();
        dto.active=clist.isActive();
        dto.genericFormat = clist.isGenericFormat();
        dto.fircosoftFormat = clist.isFircosoftFormat();
        dto.reglissFormat = clist.isReglissFormat();
        dto.safewatchFormat = clist.isSafewatchFormat();
        dto.vigilanceFormat = clist.isVigilanceFormat();
        dto.fullyEditable=clist.isFullyEditable();
        dto.canDownloadImport = false;
        dto.canDownloadExport = false;
        return dto;
    }

    public ConcatenatedList toEntityForUpdate(ConcatenatedList entity, ConcatenatedListDto dto) {
        entity.setLabel(dto.label);
        entity.setDeleted(dto.deleted);
        entity.setActive(dto.active);
        entity.setFircosoftFormat(dto.fircosoftFormat);
        entity.setGenericFormat(dto.genericFormat);
        entity.setReglissFormat(dto.reglissFormat);
        entity.setVigilanceFormat(dto.vigilanceFormat);
        entity.setSafewatchFormat(dto.safewatchFormat);
        entity.setFullyEditable(dto.fullyEditable);
        return entity;
    }

    public ConcatenatedList toEntityForCreate(ConcatenatedListDto dto){
        ConcatenatedList concatenatedList = new ConcatenatedList();
        concatenatedList.setLabel(dto.label);
        concatenatedList.setDeleted(false);
        concatenatedList.setActive(dto.active);
        concatenatedList.setFircosoftFormat(dto.fircosoftFormat);
        concatenatedList.setGenericFormat(dto.genericFormat);
        concatenatedList.setReglissFormat(dto.reglissFormat);
        concatenatedList.setVigilanceFormat(dto.vigilanceFormat);
        concatenatedList.setSafewatchFormat(dto.safewatchFormat);
        concatenatedList.setFullyEditable(true);
        return concatenatedList;
    }

    public ConcatenatedListUniqueFieldsDto toConcatenatedListUniqueFieldsDto(ConcatenatedListUniqueFieldsVO vo){
        ConcatenatedListUniqueFieldsDto dto = new ConcatenatedListUniqueFieldsDto();
        dto.id = vo.getId();
        dto.label = vo.getLabel();
        return dto;
    }

    public ConcatenatedListDisplayDto toConcatenatedListDisplayDto(ConcatenatedListDisplayVO vo){
        ConcatenatedListDisplayDto dto = new ConcatenatedListDisplayDto(vo.getId(), vo.getLabel(), vo.getPublicationDate(), vo.isActive());
        return dto;
    }
}
