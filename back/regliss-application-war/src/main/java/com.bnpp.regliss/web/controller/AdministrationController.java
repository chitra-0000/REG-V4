package com.bnpp.regliss.web.controller;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import com.bnpp.regliss.entity.BatchJobType;
import com.bnpp.regliss.entity.ExportFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.*;

import com.bnpp.regliss.entity.referential.*;
import com.bnpp.regliss.facade.*;
import com.bnpp.regliss.facade.dto.*;
import com.bnpp.regliss.facade.dto.administration.*;
import com.bnpp.regliss.profile.*;
import com.bnpp.regliss.repository.pagination.*;
import com.bnpp.regliss.vo.*;
import com.bnpp.regliss.web.filter.*;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("api/administration")
@ReglissWebProfile
@Slf4j
public class AdministrationController {

    @Autowired
    private AdministrationFacade facade;

    @Autowired
    private FrontParametersFacade paramsFacade;

    @SecuredFeature(FeaturePermission.substitutionMatrixView)
    @GetMapping(value="substitution-matrix/{fileFormat}")
    public List<SubstitutionMatrixElementDto> getSubstitutionMatrix(@PathVariable("fileFormat") FileExportFormat fileFormat){
        return facade.getSubstitutionMatrix(fileFormat);
    }

    @SecuredFeature(FeaturePermission.substitutionMatrixView)
    @GetMapping(value="substitution-matrix")
    public List<SubstitutionMatrixElementDetailsDto> getAllSubstitutionMatrix(){
        return facade.getAllSubstitutionMatrix();
    }


    @SecuredFeature(FeaturePermission.substitutionMatrixEdit)
    @PostMapping(value="substitution-matrix/add-substitution")
    public SingleValue addSubstitution(@RequestParam("fileFormat") FileExportFormat fileFormat,
                                       @RequestBody SubstitutionMatrixElementDto dto) {
        SingleValue newId = facade.addSubstitution(dto, fileFormat);
        return newId;
    }

    @SecuredFeature(FeaturePermission.substitutionMatrixEdit)
    @DeleteMapping("substitution-matrix/delete-substitution")
    public void deleteSubstitution(@RequestParam("substitutionId") Long substitutionId) {

        facade.deleteSubstitution(substitutionId);
    }

    @SecuredFeature(FeaturePermission.substitutionMatrixEdit)
    @PostMapping(value="substitution-matrix/{fileFormat}")
    public void crudSubstitutionMatrix(@PathVariable("fileFormat") FileExportFormat fileFormat,
            @RequestBody List<SubstitutionMatrixElementDto> dto) {
        facade.crudSubstitutionMatrix(dto, fileFormat);
    }

    @SecuredFeature(FeaturePermission.djImportAdmin)
    @PostMapping("send-cftgtw-request")
    public void sendCftgtwRequest() {
        facade.scheduleCftGtwRequest();
    }

    @SecuredFeature(FeaturePermission.djImportAdmin)
    @PostMapping("send-cftgtw-request-of-date")
    public void sendCftgtwRequestOfDate(@RequestParam("reqDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reqDate){
        log.info("Requested schedule for date: ",reqDate.atStartOfDay());
        facade.scheduleCftGtwRequestAsOfDate(reqDate.atStartOfDay());
    }

    @SecuredFeature(FeaturePermission.djImportAdmin)
    @PostMapping("send-six-cftgtw-request-of-date")
    public void sendSixCftgtwRequestOfDate(@RequestParam("reqDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reqDate,@RequestParam("slot") String slot){
        log.info("Requested schedule for date: ",reqDate.atStartOfDay());
        facade.scheduleSixCftGtwRequestAsOfDate(reqDate.atStartOfDay(),slot);
    }


    @SecuredFeature(FeaturePermission.djImportAdmin)
    @GetMapping("latestWLImportOKDate")
    public LocalDate getLatestWLImportOKDate() {
        return facade.getLatestWLImportOKDate();
    }

    @SecuredFeature(FeaturePermission.djImportAdmin)
    @PostMapping(value = "upload-dj-file")
    public void uploadDjFile(@RequestParam("file") MultipartFile file) throws IOException {
        facade.uploadDjFile(file.getOriginalFilename(), file.getInputStream());
    }

    @SecuredFeature(FeaturePermission.djImportAdmin)
    @PostMapping(value = "upload-dj-files")
    public void uploadDjFiles(@RequestParam("files")List<MultipartFile> multipartFileList) throws IOException {
        for (MultipartFile file: multipartFileList) {
            facade.uploadDjFile(file.getOriginalFilename(), file.getInputStream());
        }
    }

    @PostMapping("/get-all-jobs")
    public PaginatedResults<BatchJobExecutionDto> getAllBatchJobExecutions(@RequestBody PaginationOptions paginationOptions) {
        return facade.getAllBatchJobExecutions(paginationOptions);
    }

    @GetMapping("/get-all-jobs-no-pagination")
    public List<BatchJobExecutionDto> getAllBatchJobExecutionsNoPagination() {
        return facade.getAllBatchJobExecutionsNoPagination();
    }

    @GetMapping("/count-batch-jobs")
    public Long countAllBatchJobExecutions(@RequestParam(required = false)List<BatchJobType> jobType,
                                           @RequestParam(required = false)List<ExportFile.GenerationStatus> status,
                                           @RequestParam(required = false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
                                           @RequestParam(required = false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)LocalDateTime dateTo) {
        return facade.countAllBatchJobExecutions(jobType,
                status,
                dateFrom, dateTo);
    }
    @GetMapping("/get-all-jobs-with-pagination")
    public List<BatchJobExecutionDto> getAllBatchJobExecutionsWithPagination(@RequestParam(name = "page") Integer page,
                                                                             @RequestParam(name = "size") Integer size,
                                                                             @RequestParam(required = false)List<BatchJobType> jobType,
                                                                             @RequestParam(required = false)List<ExportFile.GenerationStatus> status,
                                                                             @RequestParam(required = false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)LocalDateTime dateFrom,
                                                                             @RequestParam(required = false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)LocalDateTime dateTo) {
        return facade.getAllBatchJobExecutionsWithPagination(page, size,
                jobType,
                status,
                dateFrom, dateTo);

    }

    @PostMapping("unblock-job-execution/{id}")
    public void unblockJobExecution(@PathVariable("id") Long batchJobExecutionId) {
        facade.unblockJobExecution(batchJobExecutionId);
    }

    @GetMapping(value="parameters")
    public FrontParametersDto getFEParameters() {
        return paramsFacade.getParameters();
    }

    @SecuredFeature(FeaturePermission.userUploadGuide)
    @PostMapping(value = "upload-user-guide")
    public void uploadUserGuide(@RequestParam("files")List<MultipartFile> multipartFileList)  {
        multipartFileList.forEach(f->facade.uploadUserGuide(f));
    }

    @GetMapping("download-user-guide")
    public void downloadUserGuideFile(HttpServletResponse response) throws IOException {
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + facade.getUserGuideFileName() + "\"");
        facade.downloadUserGuideFile(response.getOutputStream());
    }


    @GetMapping("user-guide")
    public UserGuideDto getUserGuide() {
        return facade.getUserGuide();
    }

}

