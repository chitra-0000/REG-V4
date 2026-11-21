package com.bnpp.regliss.web.controller;

import com.bnpp.regliss.entity.referential.FileExportExtension;
import com.bnpp.regliss.facade.AddModSupFacade;
import com.bnpp.regliss.facade.DownloadFileFacade;
import com.bnpp.regliss.facade.GeneratedFileFacade;
import com.bnpp.regliss.facade.dto.version.AddModSupAuditTrailFileDto;
import com.bnpp.regliss.facade.dto.version.AddModSupExportFileDto;
import com.bnpp.regliss.facade.dto.version.GeneratedFileDto;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.service.permission.CurrentVersionPermissions;
import com.bnpp.regliss.vo.FeaturePermission;
import com.bnpp.regliss.web.filter.SecuredFeature;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("api/generated-files")
@ReglissWebProfile
public class DownloadFileController {

    @Autowired
    private GeneratedFileFacade generateFileFacade;

    @Autowired
    private CurrentVersionPermissions versionPermissions;

    @Autowired
    private DownloadFileFacade downloadFileFacade;

    @Autowired
    private AddModSupFacade addModSupFacade;

    private static final String CONTENT_DISPOSITION = "Content-Disposition";
    private static final String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";
    private static final String HEADER_CONTENT_DISPOSITION = "attachment; filename=";
    private static final String CRB_IMPORT_FILE_EXTENSION = ".xlsx";

    @SecuredFeature(FeaturePermission.userDownloadExportFilesAllowed)
    @GetMapping("{id}")
    public void downloadFile(@PathVariable Long id, HttpServletResponse response) throws IOException {
        // TODO permission: addAndTestIfNew user has permission on the list to which the file belongs.
        GeneratedFileDto dto = generateFileFacade.getGeneratedFile(id);
        if (dto.versionId != null) {
            versionPermissions.checkConsultVersionAccess(dto.versionId);
        }
        String filename = dto.fileName + "." + dto.extension;
        response.setContentType(computeContentType(dto.extension));
        response.addHeader(CONTENT_DISPOSITION, HEADER_CONTENT_DISPOSITION + filename);

        downloadFileFacade.downloadGeneratedFile(id, response.getOutputStream());
    }

    @SecuredFeature(FeaturePermission.userDownloadImportFilesAllowed)
    @GetMapping("/{id}/download-imported-file")
    public void downloadImportedFile(@PathVariable Long id, HttpServletResponse response) throws IOException {
        versionPermissions.checkConsultVersionAccess(id);
        String originalFileName  = downloadFileFacade.getImportedFileName(id);
        response.setContentType(CONTENT_TYPE_OCTET_STREAM);
        if (originalFileName != null) {
            if (originalFileName.endsWith(CRB_IMPORT_FILE_EXTENSION)) {
                response.addHeader(CONTENT_DISPOSITION, HEADER_CONTENT_DISPOSITION + originalFileName + ".zip");
            } else {
                response.addHeader(CONTENT_DISPOSITION, computeContentDisposition(originalFileName, getFileExtension(originalFileName)));
            }
            downloadFileFacade.downloadImportFile(id, response.getOutputStream());
        }
    }

    @SecuredFeature(FeaturePermission.userDownloadExportFilesAllowed)
    @GetMapping("/{versionId}/download-add-mod-sup")
    public void downloadAddModSup(@PathVariable Long versionId, HttpServletResponse response) throws IOException {
        versionPermissions.checkConsultVersionAccess(versionId);
        AddModSupExportFileDto addModSupExportFileDto = addModSupFacade.getAddModSupExportFile(versionId);
        response.setContentType(CONTENT_TYPE_OCTET_STREAM);
        response.addHeader(CONTENT_DISPOSITION,
                "attachment; filename=\"" + addModSupExportFileDto.fileName + ".xlsx\"");
        downloadFileFacade.downloadAddModSupFile(addModSupExportFileDto.id, response.getOutputStream());
    }

    @SecuredFeature(FeaturePermission.userDownloadVerificationCommentsAllowed)
    @GetMapping("/{versionId}/download-add-mod-sup-verifications-comments")
    public void downloadAddModSupAuditTrail(@PathVariable Long versionId, HttpServletResponse response) throws IOException {
        versionPermissions.checkConsultVersionAccess(versionId);
        AddModSupAuditTrailFileDto addModSupAuditTrailFileDto = addModSupFacade.getAddModSupAuditTrailFile(versionId);
        response.setContentType(CONTENT_TYPE_OCTET_STREAM);
        response.addHeader(CONTENT_DISPOSITION,
                "attachment; filename=\"" + addModSupAuditTrailFileDto.fileName + ".xlsx\"");
        downloadFileFacade.downloadAddModSupAuditTrailFile(addModSupAuditTrailFileDto.id, response.getOutputStream());
    }

    private String computeContentType(FileExportExtension extension) {
        if (extension == FileExportExtension.zip) {
            return CONTENT_TYPE_OCTET_STREAM;
        } else {
            return "text/" + extension;
        }
    }

    private FileExportExtension getFileExtension(String fileName) {
        if (Strings.isEmpty(fileName) || Strings.isBlank(fileName)) {
            return null;
        }
        return FileExportExtension.valueOf(fileName.toLowerCase().substring(fileName.lastIndexOf(".") + 1));
    }

    private String computeContentDisposition(String fileName, FileExportExtension extension) {
        if (extension == FileExportExtension.zip) {
            return HEADER_CONTENT_DISPOSITION + fileName;
        } else {
            return HEADER_CONTENT_DISPOSITION + fileName + ".zip";
        }
    }

}

