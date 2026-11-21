package com.bnpp.regliss.web.controller;

import com.bnpp.regliss.config.controller.ExternalAPILog;
import com.bnpp.regliss.facade.ExportToPdfFacade;
import com.bnpp.regliss.facade.RecordExternalFacade;
import com.bnpp.regliss.facade.RecordFacade;
import com.bnpp.regliss.facade.ReferenceFacade;
import com.bnpp.regliss.facade.dto.record.RecordAssociationDto;
import com.bnpp.regliss.facade.dto.record.RecordDescriptionDto;
import com.bnpp.regliss.facade.dto.record.RecordDto;
import com.bnpp.regliss.facade.dto.reference.ReferentialType;
import com.bnpp.regliss.profile.ReglissExternalAPIProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/external")
@ReglissExternalAPIProfile
@ExternalAPILog
//ATTENTION: URL accessed directly via URL from external apps
public class ExternalRecordDetailAPIController {
    @Autowired
    private RecordExternalFacade facade;
    @Autowired
    private ReferenceFacade referenceFacade;
    @Autowired
    private RecordFacade recordFacade;

    @GetMapping("records/{uuid}")
    public RecordDto getRecord(@PathVariable String uuid, HttpServletRequest req) {
        return facade.getRecord(uuid);
    }

    @GetMapping("records/descriptions/all")
    public List<RecordDescriptionDto> getDescriptionsCombinations() {
        return facade.getAllDescriptions();
    }

    @GetMapping("list-and-record-referential")
    public Map<ReferentialType, List<Object>> getListAndRecordReferential(HttpServletRequest req) {
        return referenceFacade.retrieveListAndRecordReferential();
    }

    @GetMapping("reference")
    public Map<ReferentialType, List<Object>> getAllReferenceData(HttpServletRequest req) {
        return referenceFacade.retrieveUITranslationReferential();
    }

    @GetMapping("records/{uuid}/associations")
    public List<RecordAssociationDto> getRecordAssociations(@PathVariable String uuid, HttpServletRequest req){
        return recordFacade.getRecordAssociations(uuid);
    }

    @GetMapping(value = "records/{uuid}/pdf",produces = "application/pdf")
    public ResponseEntity<byte[]> export(@PathVariable String uuid) {
        ByteArrayOutputStream reportTempPath = facade.exportPdf(uuid);
        HttpHeaders respHeaders = facade.configureHttpContent("report_reglis_detailed");
        return ResponseEntity.ok().headers(respHeaders).body(reportTempPath.toByteArray());
    }

}

