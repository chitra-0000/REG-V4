package com.bnpp.regliss.exporter.users;

import static java.util.stream.Collectors.joining;

import java.io.OutputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import com.bnpp.regliss.entity.Language;
import com.bnpp.regliss.entity.User;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.service.EmailService;
import com.bnpp.regliss.service.ReferenceService;
import com.bnpp.regliss.service.ReglissRequestContext;
import com.bnpp.regliss.vo.UserWithAccessibleListsVO;

@Service
@ReglissWebProfile
public class UserSearchExportService {

    private static final String HEADER_KEY = "UserSearchExportService.USER_EXPORT_HEADER";

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private ReglissRequestContext reglissContext;

    @Autowired
    private ReferenceService referenceService;

    @Autowired
    private EmailService emailService;

    public void export(List<UserWithAccessibleListsVO> usersToExport, OutputStream outputStream) {
        XlsExporter xlsExporter;
        try {
            xlsExporter = new XlsExporter(outputStream, "REGLISS-Export-Users");
            writeColumnHeaders(xlsExporter);
            writeTableContents(xlsExporter, usersToExport);
            xlsExporter.writeWorkbook();
        } catch (Exception e) {
            this.emailService.sendEmailExportError(e.getMessage(), null);
        }
    }

    private void writeColumnHeaders(XlsExporter xlsExporter) { xlsExporter.writeLine(0, translateHeader()); }

    private void writeTableContents(XlsExporter xlsExporter, List<UserWithAccessibleListsVO> usersToExport) {
        for (int i = 0; i < usersToExport.size(); i++) {
            User user = usersToExport.get(i).getUser();
            //start to write on the second row, the first row is for the header
            xlsExporter.writeLine(i + 1,
                    user.getUsername(),
                    user.getLastName(),
                    user.getFirstName(),
                    referenceService.translateEnum(user.getProfile()),
                    user.getCountry().getLabel(),
                    user.getPole(),
                    user.getEntity(),
                    user.getBusinessLine(),
                    usersToExport.get(i).getListNames().stream().collect(joining(", ")),
                    translateBoolean(user.hasExportedFileDownloadPermissionNow()),
                    translateBoolean(user.hasImportedFileDownloadPermissionNow()),
                    referenceService.translateEnum(user.getStatus()));
        }
    }

    private String translateBoolean(boolean b) {
        if (reglissContext.getLanguage() == Language.FR) {
            return b ? "Oui" : "Non";
        } else {
            return b ? "Yes" : "No";
        }
    }

    private String translateHeader() {
        return messageSource.getMessage(HEADER_KEY, null, reglissContext.getLanguage().toLocale());
    }
}

