package com.bnpp.regliss.web.controller;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.ReglissException.ErrorCode;
import com.bnpp.regliss.facade.RecordFacade;
import com.bnpp.regliss.facade.dto.RecordCountSearchDto;
import com.bnpp.regliss.facade.dto.SingleValue;
import com.bnpp.regliss.facade.dto.record.*;
import com.bnpp.regliss.facade.dto.version.RecordInVersionSearchResultDto;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.repository.pagination.PaginatedResults;
import com.bnpp.regliss.search.RecordCompareVersionsCriteria;
import com.bnpp.regliss.search.RecordSearchCriteria;
import com.bnpp.regliss.search.RecordSearchVersionCriteria;
import com.bnpp.regliss.service.ReglissRequestContext;
import com.bnpp.regliss.service.permission.CurrentVersionPermissions;
import com.bnpp.regliss.vo.FeaturePermission;
import com.bnpp.regliss.web.filter.SecuredFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Slf4j
@RestController
@RequestMapping("api/records")
@ReglissWebProfile
public class RecordController {

    @Autowired
    private RecordFacade facade;

    @Autowired
    private ReglissRequestContext requestContext;

    @Autowired
    private CurrentVersionPermissions versionPermissions;

    @PostMapping("/search-for-version")
    public PaginatedResults<RecordInVersionSearchResultDto> searchRecordsForVersion(@RequestBody RecordSearchVersionCriteria dto){
        versionPermissions.checkConsultVersionAccess(dto.idVersion);
        return facade.searchRecordsForVersion(dto);
    }

    // ATTENTION: URL accessed directly via URL from extrnal apps
    @GetMapping("/{uuid}")
    public RecordDto getRecord(@PathVariable String uuid, @RequestParam("idVersion") Long idVersion){
        return facade.getRecord(uuid, idVersion);
    }

    @GetMapping("/{reglissReference}/view")
    public RecordDto getRecordView(@PathVariable String reglissReference, @RequestParam("idVersion") Long idVersion){
        return facade.getRecordView(reglissReference, idVersion);
    }
    @GetMapping("/{reglissReference}/viewbyversionno")
    public RecordDto getRecordByReferenceAndVersionNo(@PathVariable String reglissReference, @RequestParam("versionNo") Long versionNo, @RequestParam("listId") Long listId){
        return facade.getRecordByReferenceAndVersionNo(reglissReference, versionNo, listId);
    }

    @GetMapping("/{uuid}/associations")
    public List<RecordAssociationDto> getRecordAssociations(@PathVariable String uuid){
        return facade.getRecordAssociations(uuid);
    }

    @SecuredFeature(FeaturePermission.recordUpdate)
    @PutMapping
    public SingleValue updateRecord(@RequestBody RecordDto dto){
        if (!versionPermissions.getActions(dto.generalFields.idVersion).editRecords) {
            throw new ReglissException(ErrorCode.LOST_LOCK);
        }
        return new SingleValue(facade.updateRecord(dto));
    }

    @SecuredFeature(FeaturePermission.recordUpdate)
    @PostMapping
    public void createRecord(@RequestBody RecordDto dto) {
        if (!versionPermissions.getActions(dto.generalFields.idVersion).editRecords) {
            throw new ReglissException(ErrorCode.LOST_LOCK);
        }
        log.debug("On create {}",dto.personFields.countryDetails.stream().map(cd->cd.country+"-"+cd.id+"-"+cd.type).collect(java.util.stream.Collectors.joining("")));
        facade.createRecord(dto);
    }

    @SecuredFeature(FeaturePermission.recordUpdate)
    @DeleteMapping("/{uuid}")
    public void deleteRecord(@PathVariable String uuid, @RequestParam("idVersion") Long idVersion){
        if (!versionPermissions.getActions(idVersion).editRecords) {
            throw new ReglissException(ErrorCode.LOST_LOCK);
        }
        versionPermissions.checkDeleteRecordPermission(idVersion);
        facade.deleteRecord(uuid, idVersion);
    }

    @PostMapping("/search")
    public PaginatedResults<RecordSearchResultDto> searchRecords(@RequestBody RecordSearchCriteria criteria){
        criteria.authenticatedLowLevel = !requestContext.isHighAuthentication();
        return facade.searchRecordsForPage(criteria);
    }

    @PostMapping("/search-total-items")
    public RecordCountSearchDto searchRecordsForTotalItemsCount(@RequestBody RecordSearchCriteria criteria){
        criteria.authenticatedLowLevel = !requestContext.isHighAuthentication();
        return facade.searchRecordsForTotal(criteria);
    }

    @GetMapping("/descriptions/all")
    public List<RecordDescriptionDto> getDescriptionsCombinations() {
        return facade.getAllDescriptions();
    }

    @GetMapping("/compareVersions/versionsByDate")
    public VersionsByDateDto getVersionsByDate(@RequestParam("date1") String date1,
                                               @RequestParam("date2") String date2,
                                               @RequestParam("listId") Long listId){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E MMM d yyyy", Locale.US);
        return facade.getVersionsByDate(LocalDate.parse(date1,formatter), LocalDate.parse(date2,formatter), listId);
    }


    @GetMapping("/compareVersions")
    public CompareVersionsDto getCompareVersions(@RequestParam("idVersion1") Long idVersion1,
                                                 @RequestParam("idVersion2") Long idVersion2,
                                                 @RequestParam("listId") Long listId){
        return facade.getCompareVersions(Math.min(idVersion1, idVersion2), Math.max(idVersion1, idVersion2), listId);
    }

    @PostMapping("/compareVersions")
    public CompareVersionsDto getCompareVersions(@RequestBody RecordCompareVersionsCriteria criteria){
        return facade.getCompareVersionsByCriteria(criteria);
    }

    @GetMapping("/previous-uuid")
    public SingleValue getPreviousUuid(@RequestParam("previousVersionId") Long previousVersionId,
                                       @RequestParam("uuid") String uuid){
        return new SingleValue(facade.getPreviousUuid(previousVersionId,uuid));
    }
}
