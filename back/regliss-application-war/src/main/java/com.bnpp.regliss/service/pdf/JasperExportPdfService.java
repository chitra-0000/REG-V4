package com.bnpp.regliss.service.pdf;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.config.thread.autocloseable.AutocloseableResourcesHelper;
import com.bnpp.regliss.facade.dto.record.ElementDetailExportDto;
import com.bnpp.regliss.service.ReglissRequestContext;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class JasperExportPdfService {

    private static final String REPORT_MEDIA_TYPE = "pdf";

    @Value("${jasperreports.location}")
    private String reportsLocation;

    @Autowired
    private ReglissRequestContext requestContext;
    @Autowired
    AutocloseableResourcesHelper fileHelper;
    public ByteArrayOutputStream exportPdfFile(ElementDetailExportDto detailDto) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        File pdfFile = null;
        JasperReport report =null;
        URL jasperStream =null;
        JasperPrint print =null;
        try {
            jasperStream = JasperCompileManager.class
                    .getResource("/jasperreports/detailed_results_report.jasper");
            report = (JasperReport) JRLoader.loadObject(jasperStream);

            Map<String, Object> params = new HashMap<>();
            String imagesLocation = reportsLocation + "images/";
            log.info("PDF Images Path *** ", imagesLocation);
            params.put("BNP_BANNER_LOCATION", imagesLocation);
            log.info("PDF Report Path *** " , reportsLocation);
            params.put("SUBREPORT_DIR", reportsLocation);
            params.put("REPORT_LOCALE", requestContext.getLanguage().toLocale());
             // fill out the report into a print object, ready for export.
            print = JasperFillManager.fillReport(report,
                     params,  new JRBeanCollectionDataSource(Collections.singletonList(detailDto)));
             // start export file
            pdfFile = fileHelper.createTempFileForThisThread("output", ".pdf");
        } catch (JRException e) {
            throw new ReglissException(e);
        }
            try (FileOutputStream os = new FileOutputStream(pdfFile)){
                JasperExportManager.exportReportToPdfStream(print, os);
                outputStream.write(Files.readAllBytes(pdfFile.toPath()));
            } catch (JRException | IOException e) {
                throw new ReglissException(e);
            }
        try {
            if( pdfFile.exists() && Files.deleteIfExists(pdfFile.toPath())) {
                log.debug("Finished pdfFile export temporary file is deleted");
            }
        } catch (IOException e) {
            log.error("error {}",e.getMessage());
        }




        return outputStream;
    }

    public HttpHeaders configureHttpContent(String genericReportName) {
        String currentDate = new SimpleDateFormat(" dd-MM-yyyy hh:mm").format(new Date());
        String name = genericReportName;
        String reportName = name + currentDate + "." + REPORT_MEDIA_TYPE;
        HttpHeaders respHeaders = new HttpHeaders();
        respHeaders.setContentType(MediaType.parseMediaType("application/" + REPORT_MEDIA_TYPE));
        respHeaders.setContentDispositionFormData("attachment", reportName);
        return respHeaders;
    }
}

