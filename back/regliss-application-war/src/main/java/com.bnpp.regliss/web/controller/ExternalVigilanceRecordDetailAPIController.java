package com.bnpp.regliss.web.controller;

import com.bnpp.regliss.config.controller.ExternalAPILog;
import com.bnpp.regliss.entity.Reglementation;
import com.bnpp.regliss.external.facade.dto.record.RecordVigilanceDto;
import com.bnpp.regliss.external.facade.dto.record.VigilanceRecordSearchCriteriaDto;
import com.bnpp.regliss.facade.RecordExternalFacade;
import com.bnpp.regliss.profile.ReglissExternalAPIProfile;
import com.bnpp.regliss.repository.ReglementationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("api/external/vigilance")
@ReglissExternalAPIProfile
@ExternalAPILog
//ATTENTION: URL accessed directly via URL from external apps
public class ExternalVigilanceRecordDetailAPIController {
    @Autowired
    private RecordExternalFacade facade;

    @Autowired
    private ReglementationRepository reglementationRepository;

    /**
     * This endpoints brings the version programs,
     * and should be removed after all clients start to use v2/records in production environment
     */
    @GetMapping("v1/records")
    public RecordVigilanceDto getRecordByIdAndVersion(@RequestParam String recordID, @RequestParam Long versionNo, @RequestParam String listReference) {

        return facade.getVigilanceRecordWithProgramsByRecordId(recordID, versionNo, listReference);
    }

    /**
     * Similar to v1/records but it skips the version programs labels
     */
    @GetMapping("v2/records")
    public RecordVigilanceDto getRecordByIdAndVersionNoListProgramsV2(@RequestParam String recordID, @RequestParam Long versionNo, @RequestParam String listReference) {
        return  facade.getVigilanceRecordByRecordIdV2(recordID, versionNo, listReference);
    }

    @GetMapping("v2/record/externalReference")
    public RecordVigilanceDto getRecordByExtRefAndVersionV2(@RequestParam String recordExternalRef,
                                                            @RequestParam Long versionNo, @RequestParam String listReference) {
        return facade.getVigilanceRecordByRecordExtRefV2(recordExternalRef, versionNo, listReference);
    }

    @PostMapping("v1/records/search")
    public List<RecordVigilanceDto> searchRecord(@RequestBody VigilanceRecordSearchCriteriaDto searchCriteria) {
        return facade.searchRecord(searchCriteria);
    }

    @GetMapping("v1/download/{id}")
    public String download(@PathVariable Long id, HttpServletResponse response) throws IOException {

        Reglementation entity = reglementationRepository.getExactlyOne(id);
        response.setContentType(entity.getFileContentType());
        response.setContentLength(entity.getFileData().length);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + entity.getFileName() + "\"");
        FileCopyUtils.copy(entity.getFileData(), response.getOutputStream());
        return null;

    }
}

