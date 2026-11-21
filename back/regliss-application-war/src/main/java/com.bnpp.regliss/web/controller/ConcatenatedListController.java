package com.bnpp.regliss.web.controller;

import com.bnpp.regliss.entity.referential.FileExportFormat;
import com.bnpp.regliss.facade.ConcatenatedListFacade;
import com.bnpp.regliss.facade.FileDeliveryFacade;
import com.bnpp.regliss.facade.GeneratedFileFacade;
import com.bnpp.regliss.facade.SubscriberFacade;
import com.bnpp.regliss.facade.dto.SingleValue;
import com.bnpp.regliss.facade.dto.administration.SubscriberDetailsDto;
import com.bnpp.regliss.facade.dto.administration.SubscriberDto;
import com.bnpp.regliss.facade.dto.list.ConcatenatedListDisplayDto;
import com.bnpp.regliss.facade.dto.list.ConcatenatedListDto;
import com.bnpp.regliss.facade.dto.list.ConcatenatedListUniqueFieldsDto;
import com.bnpp.regliss.facade.dto.list.ReglissListsForConcListsDto;
import com.bnpp.regliss.facade.dto.version.GeneratedFileDto;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.vo.FeaturePermission;
import com.bnpp.regliss.web.filter.SecuredFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/lists")
@ReglissWebProfile
public class ConcatenatedListController {

    @Autowired
    private ConcatenatedListFacade concatenatedListFacade;

    @Autowired
    private GeneratedFileFacade generatedFileFacade;

    @Autowired
    private SubscriberFacade subscriberFacade;

    @Autowired
    private FileDeliveryFacade fileDeliveryFacade;

    @GetMapping("/get-all-concatenated-lists")
    public List<ConcatenatedListDto> getAllConcatenatedLists(){
        return concatenatedListFacade.getAllConcatenatedLists();
    }

    @GetMapping("/{id}/concatenated-list")
    public ConcatenatedListDto getConcatenatedListCharacteristics(@PathVariable Long id){
        return concatenatedListFacade.getConcatenatedListCharacteristicsById(id);
    }

    @SecuredFeature(FeaturePermission.listEdit)
    @PutMapping("/concatenated-list")
    public void updateConcatenatedListCharacteristics(@RequestBody ConcatenatedListDto dto) {
        this.concatenatedListFacade.updateConcatenatedListCharacteristics(dto);
    }

    @SecuredFeature(FeaturePermission.listCreate)
    @PostMapping("/concatenated-list")
    public SingleValue createConcatenatedListCharacteristics(@RequestBody ConcatenatedListDto dto) {
        long newId = concatenatedListFacade.createConcatenatedListCharacteristics(dto);
        return new SingleValue(newId);
    }

    @SecuredFeature(FeaturePermission.listEdit)
    @PutMapping("/update-lists-for-concatenated-list")
    public void updateListsForConcatenatedList(@RequestParam(name = "id") Long id, @RequestBody List<Long> newListsIds) {
        this.concatenatedListFacade.updateListsForConcatenatedList(id, newListsIds);
    }

    @SecuredFeature(FeaturePermission.listEdit)
    @PutMapping("/delete-lists-for-concatenated-list")
    public void deleteLists(@RequestParam(name = "id") Long id, @RequestBody Long listIdToDelete) {
        this.concatenatedListFacade.deleteListsForConcatenatedList(id, listIdToDelete);
    }

    @GetMapping("/get-list-for-concatenated-list")
    public List<ReglissListsForConcListsDto> getReglissListForConcatenatedList(@RequestParam(name = "id") Long id){
        return this.concatenatedListFacade.getReglissListForConcatenatedList(id);
    }

    @GetMapping("/generated-files")
    public List<GeneratedFileDto> getGeneratedFilesForConcatenatedList(@RequestParam(name = "id") Long id) {
        return generatedFileFacade.getLatestGeneratedFilesForConcatenatedList(id);
    }

    @SecuredFeature(FeaturePermission.generateExportForConcatenatedList)
    @PutMapping(value="/{id}/regenerate-files")
    public void regenerateFiles(@PathVariable Long id, @RequestBody List<FileExportFormat> formats) {
        generatedFileFacade.regenerateConcatenatedExportFiles(id, formats);
    }

    @GetMapping("/{id}/get-export-formats-to-regenerate")
    public List<FileExportFormat> getExportFormatsToRegenerate(@PathVariable Long id){
        return generatedFileFacade.getConcatenatedExportFormatsToRegenerate(id);
    }

    @SecuredFeature(FeaturePermission.listDelete)
    @PutMapping("/delete-concatenated-list")
    public void deleteConcatenatedList(@RequestParam(name="id") Long id){
        this.concatenatedListFacade.deleteConcatenatedList(id);
    }

    @GetMapping("/{id}/get-subscribers")
    public List<SubscriberDto> getSubscribersForConcatenatedList(@PathVariable Long id){
        return this.subscriberFacade.getSubscribersForConcatenatedList(id);
    }

    @SecuredFeature(FeaturePermission.redeliverFilesForConcatenatedList)
    @PostMapping("/{id}/redeliver-files")
    public void redeliverFiles(@PathVariable Long id, @RequestBody List<Long> subscriberIds) {
        fileDeliveryFacade.redeliverFilesForConcatenatedList(id, subscriberIds);
    }

    @GetMapping("/get-unique-labels-for-concatenated-list")
    public List<ConcatenatedListUniqueFieldsDto> getUniqueFields() {
        return concatenatedListFacade.getUniqueFields();
    }

    @GetMapping("/concatenated-lists")
    public List<ConcatenatedListDisplayDto> getAllConcatenatedListsToDisplay(){
        return concatenatedListFacade.getAllConcatenatedListsToDisplay();
    }
}

