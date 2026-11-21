package com.bnpp.regliss.web.controller;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.entity.Reglementation;
import com.bnpp.regliss.facade.ReglementationFacade;
import com.bnpp.regliss.facade.dto.SingleValue;
import com.bnpp.regliss.facade.dto.version.BaseReglementationDto;
import com.bnpp.regliss.facade.dto.version.ReglementationDto;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.repository.ReglementationRepository;
import com.bnpp.regliss.service.permission.CurrentListPermissions;
import com.bnpp.regliss.service.permission.CurrentVersionPermissions;
import com.bnpp.regliss.service.verification.FileTypeVerificationService;
import com.bnpp.regliss.vo.FeaturePermission;
import com.bnpp.regliss.web.boot.CustomJacksonObjectMapper;
import com.bnpp.regliss.web.filter.SecuredFeature;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("api/reglementation")
@ReglissWebProfile
public class ReglementationController {

    @Autowired
    private ReglementationFacade facade;

    @Autowired
    private ReglementationRepository repository;

    @Autowired
    private CurrentVersionPermissions versionPermissions;

    @Autowired
    private CurrentListPermissions listPermissions;

    @Autowired
    private FileTypeVerificationService fileTypeVerificationService;

    @SecuredFeature({FeaturePermission.versionReglementationEdit})
    @PostMapping(path = "/version/{versionId}", consumes = { "multipart/form-data", "application/json" }, produces = "application/json")
    @ResponseBody
    public SingleValue createForVersion(
            @PathVariable Long versionId,
            @RequestParam(value = "params") String dtoStr,
            @RequestParam(value = "file", required=false) MultipartFile file) throws IOException {
        fileTypeVerificationService.checkNameAndType(file);

        CustomJacksonObjectMapper mapper = new CustomJacksonObjectMapper();
        ReglementationDto dto = mapper.readValue(dtoStr, ReglementationDto.class);

        if (dto.id == null) {
            versionPermissions.checkConsultVersionAccess(versionId);
            return new SingleValue(facade.createForVersion(dto, versionId, file));
        } else {
            versionPermissions.checkReglementationAccess(dto.id, versionId);
            facade.updateForVersion(dto, versionId, file);
            return new SingleValue(dto.id);
        }
    }

    @SecuredFeature({FeaturePermission.listReglementationEdit})
    @PostMapping(path = "/list", consumes = { "multipart/form-data", "application/json" }, produces = "application/json")
    @ResponseBody
    public SingleValue createForList(
            @RequestParam(value = "params") String dtoStr,
            @RequestParam(value = "file", required=false) MultipartFile file) throws IOException {
        fileTypeVerificationService.checkNameAndType(file);

        CustomJacksonObjectMapper mapper = new CustomJacksonObjectMapper();
        ReglementationDto dto = mapper.readValue(dtoStr, ReglementationDto.class);
        listPermissions.checkListAccess(dto.listId);

        if (dto.id == null) {
            return new SingleValue(facade.createForList(dto, file));
        } else {
            facade.updateForList(dto, file);
            return new SingleValue(dto.id);
        }
    }

    @GetMapping("/version")
    public List<BaseReglementationDto> getReglementationsForVersion(@RequestParam("idVersion") Long idVersion) {
        versionPermissions.checkConsultVersionAccess(idVersion);
        return facade.getReglementationsForVersion(idVersion);
    }

    @GetMapping("/{reglementationId}/version/{versionId}")
    public ReglementationDto getReglementationInVersion(@PathVariable Long reglementationId, @PathVariable Long versionId) {
        versionPermissions.checkReglementationAccess(reglementationId, versionId);
        return facade.getReglementation(reglementationId);
    }

    @SecuredFeature(FeaturePermission.versionReglementationDownload)
    @GetMapping("/{reglementationId}/version/{versionId}/download")
    public void downloadInVersion(@PathVariable Long reglementationId, @PathVariable Long versionId, HttpServletResponse response) throws IOException {
        versionPermissions.checkReglementationAccess(reglementationId, versionId);
        downloadReglementation(reglementationId, response);
    }

    @SecuredFeature(FeaturePermission.versionReglementationEdit)
    @DeleteMapping("/{reglementationId}/version/{versionId}")
    public void deleteInVersion(@PathVariable Long reglementationId, @PathVariable Long versionId) {
        versionPermissions.checkReglementationAccess(reglementationId, versionId);
        facade.deleteForVersion(reglementationId, versionId);
    }

    @GetMapping("/list")
    public List<BaseReglementationDto> getReglementationsForList(@RequestParam("idList") Long idList) {
        listPermissions.checkListAccess(idList);
        return facade.getReglementationsForList(idList);
    }

    @GetMapping("{reglementationId}/list/{listId}")
    public ReglementationDto getReglementationInList(@PathVariable Long reglementationId, @PathVariable Long listId) {
        listPermissions.checkListAccess(listId);
        return facade.getReglementation(reglementationId);
    }

    @SecuredFeature(FeaturePermission.listReglementationDownload)
    @GetMapping("/list/download/{id}")
    public String downloadInList(@PathVariable Long id, HttpServletResponse response) throws IOException {
        listPermissions.checkListReglementationAccess(id);
        downloadReglementation(id, response);
        return null;
    }

    private void downloadReglementation(@PathVariable Long id, HttpServletResponse response) throws IOException {
        Reglementation entity = repository.getExactlyOne(id);
        response.setContentType(entity.getFileContentType());
        response.setContentLength(entity.getFileData().length);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + entity.getFileName() + "\"");
        FileCopyUtils.copy(entity.getFileData(), response.getOutputStream());
    }

    @SecuredFeature(FeaturePermission.listReglementationEdit)
    @DeleteMapping("/{reglementationId}/list/{listId}")
    public void deleteInList(@PathVariable Long reglementationId) {
        listPermissions.checkListReglementationAccess(reglementationId);
        facade.deleteForList(reglementationId);
    }

    @SecuredFeature(FeaturePermission.listReglementationEdit)
    @DeleteMapping("/{reglementationId}/list")
    public void deleteReglementation(@PathVariable Long reglementationId) {
        listPermissions.checkListReglementationAccess(reglementationId);
        facade.deleteForList(reglementationId);
    }
}

