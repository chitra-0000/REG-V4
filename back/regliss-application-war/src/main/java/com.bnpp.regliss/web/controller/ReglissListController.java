package com.bnpp.regliss.web.controller;

import com.bnpp.regliss.entity.ImportFileType;
import com.bnpp.regliss.facade.ManualImportFacade;
import com.bnpp.regliss.facade.ReglissListFacade;
import com.bnpp.regliss.facade.SubscriberFacade;
import com.bnpp.regliss.facade.dto.AllListsIncludedInConcatenatedList;
import com.bnpp.regliss.facade.dto.ListBreadcrumbDto;
import com.bnpp.regliss.facade.dto.SingleValue;
import com.bnpp.regliss.facade.dto.administration.SubscriberDto;
import com.bnpp.regliss.facade.dto.list.*;
import com.bnpp.regliss.facade.dto.record.ProgramDto;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.search.ReglissListSearchCriteria;
import com.bnpp.regliss.search.ReglissListSearchResult;
import com.bnpp.regliss.service.permission.CurrentListPermissions;
import com.bnpp.regliss.six.dto.*;
import com.bnpp.regliss.vo.FeaturePermission;
import com.bnpp.regliss.web.filter.SecuredFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("api/lists")
@ReglissWebProfile
public class ReglissListController {

    @Autowired
    private ReglissListFacade listFacade;

    @Autowired
    private SubscriberFacade subscriberFacade;

    @Autowired
    private ManualImportFacade mappingFileFacade;

    @Autowired
    private CurrentListPermissions listPermissions;

    @GetMapping("/get-unique-labels")
    public List<ReglissListUniqueFieldsDto> getUniqueFields() {
        return listFacade.getUniqueFields();
    }

    @SecuredFeature({FeaturePermission.listCreate, FeaturePermission.listEdit})
    @GetMapping("/does-ref-exist")
    public boolean doesNewRefExist(@RequestParam(value = "listId", required = false) Long listId, @RequestParam("reference") String ref) {
        return listFacade.doesNewRefExist(listId, ref);
    }

    @GetMapping("/names")
    public List<ReglissListShortNameDto> getShortNames() {
        return listFacade.getAllListShortNamesAndIds();
    }

    @GetMapping("/{id}")
    public ReglissListDto getList(@PathVariable Long id){
        listPermissions.checkListAccess(id);
        return listFacade.getList(id);
    }

    @GetMapping("/characteristics/{id}")
    public ReglissListCharacteristicsDto getListCharacteristics(@PathVariable Long id){
        listPermissions.checkListAccess(id);
        return listFacade.getListCharacteristicsById(id);
    }

    @SecuredFeature(FeaturePermission.listCreate)
    @PostMapping("/characteristics")
    public SingleValue createListCharacteristics(@RequestBody ReglissListCharacteristicsDto dto) {
        long newId = listFacade.createListCharacteristics(dto);
        return new SingleValue(newId);
    }

    @SecuredFeature(FeaturePermission.listEdit)
    @PutMapping("/characteristics")
    public void updateListCharacteristics(@RequestBody ReglissListCharacteristicsDto dto) {
        listPermissions.checkListAccess(dto.id);
        this.listFacade.updateListCharacteristics(dto);
    }

    @GetMapping("/import-export/{id}")
    public ReglissListImportExportDto getListImportExport(@PathVariable Long id){
        listPermissions.checkListAccess(id);
        return listFacade.getListImportExport(id);
    }

    @PutMapping("/import-export")
    public void updateListImportExport(@RequestBody ReglissListImportExportDto dto){
        listPermissions.checkListAccess(dto.id);
        listFacade.updateListImportExport(dto);
    }

    @GetMapping("/versions/{id}")
    public List<VersionForVersionPageDto> getVersionsForVersionPage(@PathVariable Long id){
        listPermissions.checkListAccess(id);
        return listFacade.getVersionsForVersionPage(id);
    }

    @GetMapping("/breadcrumb-for-list/{id}")
    public ListBreadcrumbDto getBreadcrumbForList(@PathVariable Long id){
        listPermissions.checkListAccess(id);
        return listFacade.getBreadcrumbForList(id);
    }

    @PostMapping("/{listId}/six-filters/regenerate")
    public void regenerateFile(@PathVariable Long listId) {
        listPermissions.checkListAccess(listId);
        listFacade.regenerateFile(listId);
    }

    @GetMapping("/{listId}/six-filters/regenerate/available")
    public boolean isSixRegenerationAvailable(@PathVariable Long listId) {
        listPermissions.checkListAccess(listId);
        return listFacade.isSixRegenerationAvailable();
    }

    @SecuredFeature(FeaturePermission.listCreate)
    @PostMapping
    public SingleValue create(@RequestBody ReglissListDto dto) {
        long newId = listFacade.create(dto);
        return new SingleValue(newId);
    }

    @SecuredFeature(FeaturePermission.listEdit)
    @PutMapping
    public void update(@RequestBody ReglissListDto dto) {
        listPermissions.checkListAccess(dto.id);
        listFacade.update(dto);
    }

    @GetMapping("/{id}/broadcast-list")
    public BroadcastDto getBroadcastEmails(@PathVariable Long id){
        listPermissions.checkListAccess(id);
        return listFacade.getBroadcastEmails(id);
    }

    @SecuredFeature(FeaturePermission.listBroadcastListEdit)
    @PutMapping("/{id}/broadcast-list")
    public void updateBroadcastEmails(@PathVariable Long id, @RequestBody BroadcastDto broadcastDto) {
        listPermissions.checkListAccess(id);
        listFacade.updateBroadcastEmails(id, broadcastDto);
    }

    @SecuredFeature(FeaturePermission.listEdit)
    @PutMapping("/{id}/set-import-configuration")
    public void updateAutomaticImport(@PathVariable Long id, @RequestBody ImportConfigurationDto importConfigurationDto) {
        listPermissions.checkListAccess(id);
        listFacade.setImportParameters(id, importConfigurationDto);
    }

    @SecuredFeature(FeaturePermission.listDelete)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        listPermissions.checkListAccess(id);
        listFacade.delete(id);
    }

    @SecuredFeature(FeaturePermission.listInputEdit)
    @PostMapping(value = "/{id}/upload-mapping-file", consumes = "multipart/form-data")
    public MappingFileDto uploadMappingFile(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        listPermissions.checkListAccess(id);
        return mappingFileFacade.uploadCsvMappingFile(id, file.getOriginalFilename(), file.getInputStream());
    }

    @GetMapping("/{id}/number-of-crb-list-db")
    public SingleValue checkIfCRBListExists(@PathVariable Long id) {
        Boolean check = listFacade.checkIfCRBListExists(id);
        return new SingleValue(check);
    }

    @SecuredFeature(FeaturePermission.listInputEdit)
    @PostMapping("/{id}/validate-mapping-file")
    public ReglissListDto validateAndSaveMappingFile(@PathVariable Long id, @RequestBody MappingFileDto mappingFileDto) {
        listPermissions.checkListAccess(id);
        mappingFileFacade.setCsvMappingFile(id, mappingFileDto);
        return listFacade.getList(id);
    }

    @SecuredFeature(FeaturePermission.listSearch)
    @PostMapping("/search")
    public List<ReglissListSearchResult> searchReglissLists(@RequestBody ReglissListSearchCriteria criteria){
        return listFacade.searchReglissListsOptimized(criteria);
    }

    @GetMapping("/{id}/subscribers")
    public List<SubscriberDto> getSubscribers(@PathVariable Long id) {
        listPermissions.checkListAccess(id);
        return subscriberFacade.getAllSubscribersForList(id);
    }

    @GetMapping(value="/{id}/subscribers-using")
    public List<SubscriberDto> checkListReferencedBySubscribers(@PathVariable Long id) {
        listPermissions.checkListAccess(id);
        return subscriberFacade.getActiveSubscribersForList(id);
    }

    @SecuredFeature(FeaturePermission.listReglementationDownload)
    @GetMapping(value="/{id}/programs-for-reglementary")
    public List<ProgramDto> getAvailableProgramsForReglementary(@PathVariable Long id) {
        listPermissions.checkListAccess(id);
        return listFacade.getAvailableProgramsForReglementary(id);
    }

    @GetMapping("/{format}/descriptions/all")
    public List<WatchlistDescriptionAssociationDto> getDescriptionsCombinationsByFormat(
            @PathVariable ImportFileType format) {
        return listFacade.getAllDescriptionsCombinations(format);
    }

    @GetMapping("/filters/{filterId}/subscribers-using")
    public List<String> checkFilterReferencedBySubscribers(@PathVariable Long filterId) {
        return subscriberFacade.getSubscribersForFilter(filterId);
    }

    @GetMapping("/{id}/filters")
    public List<DJExportFilterDto> getFilters(@PathVariable Long id) {
        listPermissions.checkListAccess(id);
        return listFacade.getFilters(id);
    }

    @GetMapping("/{id}/filters/{filterId}")
    public DJExportFilterAndSubfiltersDto getFilter(@PathVariable Long id, @PathVariable Long filterId) {
        listPermissions.checkListAccess(id);
        return listFacade.getFilter(filterId);
    }

    @SecuredFeature(FeaturePermission.listEdit)
    @PostMapping("/{id}/filters")
    public SingleValue createExportFilter(@PathVariable Long id, @RequestBody DJExportFilterDto dto) {
        listPermissions.checkListAccess(id);
        Long filterId = listFacade.createExportFilter(id, dto);
        return new SingleValue(filterId);
    }

    @SecuredFeature(FeaturePermission.listEdit)
    @PutMapping("/{id}/filters")
    public void updateExportFilter(@PathVariable Long id, @RequestBody DJExportFilterDto dto) {
        listPermissions.checkListAccess(id);
        listFacade.updateExportFilter(id, dto);
    }

    @SecuredFeature(FeaturePermission.listEdit)
    @PutMapping("/{id}/filters/{filterId}/subfilters")
    public void updateExportSubfilters(@PathVariable Long id, @PathVariable Long filterId, @RequestBody List<DJExportSubfilterDto> dtos) {
        listPermissions.checkListAccess(id);
        listFacade.updateExportSubfilters(id, filterId, dtos);
    }

    @SecuredFeature(FeaturePermission.listEdit)
    @PutMapping("/{id}/filters/{filterId}/subfilters/{subfilterId}")
    public DJExportFilterAndSubfiltersDto updateExportSubfilter(@PathVariable Long id, @PathVariable Long filterId, @PathVariable Long subfilterId, @RequestBody DJExportSubfilterDto dto) {
        listPermissions.checkListAccess(id);
        return listFacade.updateExportSubfilter(id, filterId, subfilterId, dto);
    }

    @SecuredFeature(FeaturePermission.listEdit)
    @PostMapping("/{id}/filters/{filterId}/subfilters")
    public DJExportFilterAndSubfiltersDto createExportSubfilter(@PathVariable Long id, @PathVariable Long filterId, @RequestBody DJExportSubfilterDto dto) {
        listPermissions.checkListAccess(id);
        return listFacade.createExportSubfilter(id, filterId, dto);
    }
    @SecuredFeature(FeaturePermission.listEdit)
    @DeleteMapping("/{id}/filters/{filterId}/subfilters/{subfilterId}")
    public DJExportFilterAndSubfiltersDto deleteExportSubfilter(@PathVariable Long id, @PathVariable Long filterId, @PathVariable Long subfilterId) {
        listPermissions.checkListAccess(id);
        return listFacade.deleteExportSubfilter(id, filterId, subfilterId);
    }
    @SecuredFeature(FeaturePermission.listEdit)
    @GetMapping("/{id}/filters/{filterId}/subfilters/{subfilterId}")
    public DJExportSubfilterDto readExportSubfilter(@PathVariable Long id, @PathVariable Long filterId, @PathVariable Long subfilterId) {
        listPermissions.checkListAccess(id);
        return listFacade.readExportSubfilter(subfilterId);
    }

    @SecuredFeature(FeaturePermission.listEdit)
    @PutMapping("/{id}/filters/{filterId}/activate")
    public void activateFilter(@PathVariable Long id, @PathVariable Long filterId) {
        listPermissions.checkListAccess(id);
        listFacade.activateFilter(filterId);
    }

    @SecuredFeature(FeaturePermission.listEdit)
    @PutMapping("/{id}/filters/{filterId}/deactivate")
    public void deactivateFilter(@PathVariable Long id, @PathVariable Long filterId) {
        listPermissions.checkListAccess(id);
        listFacade.deactivateFilter(filterId);
    }

    @SecuredFeature(FeaturePermission.listEdit)
    @DeleteMapping("/{id}/filters/{filterId}")
    public void deleteFilter(@PathVariable Long id, @PathVariable Long filterId) {
        listPermissions.checkListAccess(id);
        listFacade.deleteFilter(filterId);
    }

    @SecuredFeature({FeaturePermission.listCreate, FeaturePermission.listEdit, FeaturePermission.listSearch})
    @GetMapping("/get-import-types")
    public List<ImportTypeDto> getImportTypes() {
        return listFacade.getImportTypes();
    }

    @SecuredFeature(FeaturePermission.listEdit)
    @PostMapping("/update-list-for-concatenated-list")
    public void updateListToConcatenatedList(@RequestParam(name = "listId") Long listId, @RequestBody List<Long> concatenatedListsId) {
        listFacade.updateListToConcatenatedList(listId, concatenatedListsId);
    }

    @GetMapping("/get-concatenated-lists-for-list")
    public List<ConcatenatedListDisplayDto> getConcatenatedListsForList(@RequestParam(name = "listId") Long listId){
        return listFacade.getConcatenatedListsForList(listId);
    }

    @GetMapping("/get-lists-except-ame-and-wl")
    public List<ReglissListsForConcListsDto> getReglissListsExceptAmeAndWl(){
        return listFacade.getReglissListsExceptAmeAndWl();
    }

    @GetMapping("/included-lists/{id}")
    public List<AllListsIncludedInConcatenatedList> getAllListsIncludedInConcatenatedList(@PathVariable Long id){
        return listFacade.getAllListsIncludedInConcatenatedList(id);
    }

    @GetMapping("/{id}/number-of-six-list-db")
    public SixListsCountsDto checkIfSixListExists(@PathVariable Long id) {
        return listFacade.checkIfSixListExists(id);
    }

    @GetMapping("/{listId}/six-filters")
    public List<SixFiltersDto> getSixFilters(@PathVariable Long listId) {
        listPermissions.checkListAccess(listId);
        return listFacade.getSixFilters(listId);
    }

    @GetMapping("/{listId}/six-filters/{filterId}")
    public SixFiltersAndSubFiltersDto getSixFilter(@PathVariable Long filterId, @PathVariable Long listId) {
        listPermissions.checkListAccess(listId);
        return listFacade.getSixFilter(filterId);
    }

    @SecuredFeature(FeaturePermission.listEdit)
    @PostMapping("/{listId}/six-filters")
    public SingleValue createSixFilter(@RequestBody SixFiltersDto dto, @PathVariable Long listId) {
        listPermissions.checkListAccess(listId);
        Long filterId = listFacade.createSixFilter(listId, dto);
        return new SingleValue(filterId);
    }

    @SecuredFeature(FeaturePermission.listEdit)
    @PutMapping("/{listId}/six-filters/{filterId}")
    public void updateSixFilter(@RequestBody SixFiltersDto dto, @PathVariable Long listId, @PathVariable Long filterId) {
        listPermissions.checkListAccess(listId);
        listFacade.updateSixFilter(listId, dto, filterId);
    }

    @SecuredFeature(FeaturePermission.listEdit)
    @DeleteMapping("/{listId}/six-filters/{filterId}")
    public void deleteSixFilter(@PathVariable Long listId, @PathVariable Long filterId) {
        listPermissions.checkListAccess(listId);
        listFacade.deleteSixFilter(filterId);
    }

    @SecuredFeature(FeaturePermission.listEdit)
    @PutMapping("/{listId}/six-filters/{filterId}/activate")
    public void activateSixFilter(@PathVariable Long listId, @PathVariable Long filterId) {
        listPermissions.checkListAccess(listId);
        listFacade.activateSixFilter(filterId);
    }

    @SecuredFeature(FeaturePermission.listEdit)
    @PutMapping("/{listId}/six-filters/{filterId}/deactivate")
    public void deactivateSixFilter(@PathVariable Long listId, @PathVariable Long filterId) {
        listPermissions.checkListAccess(listId);
        listFacade.deactivateSixFilter(filterId);
    }

    @SecuredFeature(FeaturePermission.listEdit)
    @GetMapping("/{listId}/six-filters/{filterId}/subfilters/{subfilterId}")
    public SixSubFiltersDto getSixSubfilter(@PathVariable Long listId, @PathVariable Long filterId, @PathVariable Long subfilterId) {
        listPermissions.checkListAccess(listId);
        return listFacade.getSixSubfilter(subfilterId);
    }

    @SecuredFeature(FeaturePermission.listEdit)
    @PostMapping("/{listId}/six-filters/{filterId}/subfilters")
    public SixFiltersAndSubFiltersDto createSixSubFilter(@RequestBody SixSubFiltersDto dto, @PathVariable Long listId, @PathVariable Long filterId) {
        listPermissions.checkListAccess(listId);
        return listFacade.createSixSubFilter(dto, listId, filterId);
    }

    @SecuredFeature(FeaturePermission.listEdit)
    @PutMapping("/{listId}/six-filters/{filterId}/subfilters/{subfilterId}")
    public SixFiltersAndSubFiltersDto updateSixSubFilter(@RequestBody SixSubFiltersDto dto, @PathVariable Long listId, @PathVariable Long filterId, @PathVariable Long subfilterId) {
        listPermissions.checkListAccess(listId);
        return listFacade.updateSixSubFilter(dto, listId, filterId, subfilterId);
    }

    @SecuredFeature(FeaturePermission.listEdit)
    @DeleteMapping("/{listId}/six-filters/{filterId}/subfilters/{subfilterId}")
    public SixFiltersAndSubFiltersDto deleteSixSubFilter(@PathVariable Long listId, @PathVariable Long filterId,  @PathVariable Long subfilterId) {
        listPermissions.checkListAccess(listId);
        return listFacade.deleteSixSubFilter(listId, filterId, subfilterId);
    }

    @GetMapping("/sixFieldNames")
    public List<SixTableFieldNamesDto> getSixTablesFieldNames() {
        return listFacade.getSixTablesFieldNames();
    }
}

