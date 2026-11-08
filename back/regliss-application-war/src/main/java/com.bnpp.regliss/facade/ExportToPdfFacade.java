package com.bnpp.regliss.facade;

import com.bnpp.regliss.config.facade.Facade;
import com.bnpp.regliss.entity.User;
import com.bnpp.regliss.facade.dto.record.ElementDetailExportDto;
import com.bnpp.regliss.facade.dto.record.RecordDto;
import com.bnpp.regliss.facade.dto.version.VersionDtoView;
import com.bnpp.regliss.facade.mapper.ExportToPdfMapperService;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.service.ReglissRequestContext;
import com.bnpp.regliss.service.pdf.JasperExportPdfService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import java.io.ByteArrayOutputStream;


@Slf4j
@Facade
@ReglissWebProfile
public class ExportToPdfFacade {
    @Autowired
    private ReglissRequestContext requestContext;

    @Autowired
    private RecordFacade recordFacade;

    @Autowired
    private VersionFacade versionFacade;

    @Autowired
    private JasperExportPdfService jasperExportPdfService;

    @Autowired
    private ExportToPdfMapperService mapperService;

    public ByteArrayOutputStream exportPdf(String uuid, Long versionId) {

        RecordDto record = recordFacade.getRecord(uuid, versionId);
        //IF WATCHLIST get assosiation
        /**record.generalFields.relations.addAll(
                mapperService.ifWatchlistAceptanceList(uuid, versionId));*/
        VersionDtoView version = versionFacade.getVersion(versionId);
        User user = requestContext.getCurrentUser();
        ElementDetailExportDto detailDto = new ElementDetailExportDto(record, user, version.breadcrumbDto, mapperService);

        return jasperExportPdfService.exportPdfFile(detailDto);
    }


    public HttpHeaders configureHttpContent(String report_reglis_detailed) {
        return jasperExportPdfService.configureHttpContent(report_reglis_detailed);
    }
}

