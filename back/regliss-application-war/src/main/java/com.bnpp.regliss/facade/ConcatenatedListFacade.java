package com.bnpp.regliss.facade;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.config.facade.Facade;
import com.bnpp.regliss.entity.ConcatenatedList;
import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.entity.UserListPermission;
import com.bnpp.regliss.facade.dto.list.*;
import com.bnpp.regliss.facade.mapper.ConcatenatedListMapper;
import org.apache.commons.collections.CollectionUtils;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.repository.ConcatenatedListRepository;
import com.bnpp.regliss.repository.ReglissListRepository;
import com.bnpp.regliss.repository.UserPermissionsRepository;
import com.bnpp.regliss.service.ReglissRequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import com.bnpp.regliss.entity.ListPermission;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
// [MISSING FROM SCREENSHOTS: lines 26-32 of the original file were not visible in the photos]
    private ConcatenatedListRepository concatenatedlistRepo;

    @Autowired
    private ConcatenatedListMapper mapper;

    @Autowired
    private ReglissListRepository reglissListRepository;

    @Autowired
    private ReglissRequestContext requestContext;

    @Autowired
    private UserPermissionsRepository userPermissionsRepo;

    public List<ConcatenatedListDto> getAllConcatenatedLists() {
        List<ConcatenatedList> list = concatenatedlistRepo.findAllNotDeletedConcatenatedLists();
        return list.stream().map(clist -> mapper.toDto(clist)).sorted(ConcatenatedListDto::compareTo).collect(Collectors.toList());
    }

    public ConcatenatedListDto getConcatenatedListCharacteristicsById(Long id) {
        ConcatenatedList concatenatedList = concatenatedlistRepo.getExactlyOne(id);
        ConcatenatedListDto dto =  mapper.toDto(concatenatedList);
        List<Long> reglissListIds = concatenatedList.getLists().stream().map(ReglissList::getId).collect(toList());
        if (reglissListIds != null && !reglissListIds.isEmpty()) {
            List<UserListPermission> allPermissionsForList;
            boolean canDownloadExport = true, canDownloadImport = true;
            for (Long listId : reglissListIds) {
                allPermissionsForList = userPermissionsRepo.findByUserAndReglissListId(requestContext.getCurrentUser(), listId);
                if(!CollectionUtils.isEmpty(allPermissionsForList)) {
                    canDownloadExport &= allPermissionsForList.stream().filter(p -> p.getPermission().equals(ListPermission.DOWNLOADEXPORT))
                            .count() > 0;
                    canDownloadImport &= allPermissionsForList.stream().filter(p -> p.getPermission().equals(ListPermission.DOWNLOADIMPORT))
                            .count() > 0;
                }
            }
            dto.canDownloadExport = canDownloadExport;
            dto.canDownloadImport = canDownloadImport;
        }
        return dto;
    }

    public void updateConcatenatedListCharacteristics(ConcatenatedListDto dto) {
        ConcatenatedList concatenatedListInDb = this.concatenatedlistRepo.getExactlyOne(dto.id);
        ConcatenatedList newConcatenatedList = mapper.toEntityForUpdate(concatenatedListInDb, dto);
        concatenatedlistRepo.save(newConcatenatedList);
    }

    public Long createConcatenatedListCharacteristics(ConcatenatedListDto dto){
        ConcatenatedList newList = mapper.toEntityForCreate(dto);

        if (concatenatedlistRepo.countByLabelIgnoreCaseAndDeletedFalse(newList.getLabel()) != 0) {
            throw new ReglissException(ReglissException.ErrorCode.LIST_NAME_ALREADY_IN_USE);
        }

        concatenatedlistRepo.save(newList);
        return newList.getId();
    }

    public void updateListsForConcatenatedList (Long id, List<Long> newListsIds){
        ConcatenatedList concatenatedList = concatenatedlistRepo.getExactlyOne(id);
        Set<ReglissList> newReglissListsForConcatenatedList = reglissListRepository.findAllById(newListsIds).stream().collect(toSet());

        concatenatedList.replaceReglissLists(newReglissListsForConcatenatedList);
        concatenatedlistRepo.save(concatenatedList);
    }

    public void deleteListsForConcatenatedList (Long id, Long listIdToDelete){
        ConcatenatedList concatenatedList = concatenatedlistRepo.getExactlyOne(id);
        Set<ReglissList> currentLists = new HashSet<>();
        currentLists.addAll(concatenatedList.getLists());
        ReglissList list = reglissListRepository.getExactlyOne(listIdToDelete);
        currentLists.remove(list);

        concatenatedList.setLists(currentLists);
    }

    public List<ReglissListsForConcListsDto> getReglissListForConcatenatedList(Long id){
        ConcatenatedList concatenatedList = concatenatedlistRepo.getExactlyOne(id);
        return concatenatedList.getLists().stream()
                .map(list -> new ReglissListsForConcListsDto(list.getId(), list.getReference(), list.getShortName(), list.getName()))
                .collect(toList());
    }

    public void deleteConcatenatedList(Long id){
        ConcatenatedList concatenatedList = concatenatedlistRepo.getExactlyOne(id);
        concatenatedList.setDeleted(true);
    }

    public List<ConcatenatedListUniqueFieldsDto> getUniqueFields() {
        return concatenatedlistRepo.getConcatListUniqueFieldsVO().stream().map(mapper::toConcatenatedListUniqueFieldsDto).collect(toList());
    }

    public List<ConcatenatedListDisplayDto> getAllConcatenatedListsToDisplay(){
        return concatenatedlistRepo.findAllNotDeletedConcatenatedListsToDisplay().stream().map(mapper::toConcatenatedListDisplayDto)
                .collect(toList());
    }
}

