package com.bnpp.regliss.exporter.writer.format.addmodsup;

import com.bnpp.regliss.batch.service.ExportJobParameters;
import com.bnpp.regliss.exporter.users.XlsExporter;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.service.ReglissRequestContext;
import com.bnpp.regliss.vo.AddModSupAction;
import com.bnpp.regliss.vo.AddModSupLine;
import com.bnpp.regliss.vo.AddModSupLineForSynthesis;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@ReglissBatchProfile
public class AddModSupAuditTrailExporter {

    private static final String HEADER_KEY = "VerificationCommentExportService.VERIFICATION_COMMENT_EXPORT_HEADER";

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private ReglissRequestContext reglissContext;

    public void generateAuditTrailFile(File tempFile, Map<AddModSupAction, List<AddModSupLineForSynthesis>> dataToExport){
        XlsExporter xlsExporter;
        try (FileOutputStream fos = new FileOutputStream(tempFile)){
            xlsExporter = new XlsExporter(fos, "Export-Verifications-Comments");
            writeColumnHeaders(xlsExporter);
            writeTableContents(xlsExporter, dataToExport);
            xlsExporter.writeWorkbook();
        } catch (IOException e) {
            log.error("Cannot generate ADD/MOD/SUPP Audit Trail file : {}", e.getMessage());
        }
    }

    private void writeColumnHeaders(XlsExporter xlsExporter) { xlsExporter.writeLine(0, translateHeader()); }

    private String translateHeader() {
        return messageSource.getMessage(HEADER_KEY, null, reglissContext.getLanguage().toLocale());
    }

    private void writeTableContents(XlsExporter xlsExporter,  Map<AddModSupAction, List<AddModSupLineForSynthesis>> dataToExport) {
        int line = 0;
        AddModSupLine addModSupLine;

        for (Map.Entry<AddModSupAction, List<AddModSupLineForSynthesis>> entry : dataToExport.entrySet()) {
            List<AddModSupLineForSynthesis> list = entry.getValue();
            for(AddModSupLineForSynthesis data : list){
                addModSupLine = data.getAddModSupLine();
                xlsExporter.writeLine(line + 1,
                        entry.getKey(),
                        addModSupLine.getAkaReglissReferenceRaw().orElse(addModSupLine.getRecordReglissReference()),
                        addModSupLine.getPrimaryName(),
                        addModSupLine.getModElement(),
                        data.getVerificationStatus(),
                        data.getVerificationComment(),
                        data.getSaveDate() == null ? null : DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(data.getSaveDate()),
                        data.getUserUid()
                );
                line++;
            }
        }
    }
}
