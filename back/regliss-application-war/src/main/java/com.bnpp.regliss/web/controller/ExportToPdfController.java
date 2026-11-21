package com.bnpp.regliss.web.controller;

import com.bnpp.regliss.facade.ExportToPdfFacade;
import com.bnpp.regliss.profile.ReglissWebProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@RequestMapping("/api/export")
@ReglissWebProfile
public class ExportToPdfController {

    @Autowired
    ExportToPdfFacade pdfFacade;

    @GetMapping(produces = "application/pdf")
    public ResponseEntity<byte[]> export(@RequestParam String uuid, @RequestParam String versionId) {

        ByteArrayOutputStream reportTempPath = pdfFacade.exportPdf(uuid, Long.valueOf(versionId));
        HttpHeaders respHeaders = pdfFacade.configureHttpContent("report_reglis_detailed");
        return ResponseEntity.ok().headers(respHeaders).body(reportTempPath.toByteArray());
    }
}

