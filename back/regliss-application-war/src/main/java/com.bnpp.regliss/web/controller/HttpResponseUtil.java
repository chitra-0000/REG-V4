package com.bnpp.regliss.web.controller;

import com.bnpp.regliss.profile.ReglissWebProfile;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@ReglissWebProfile
public class HttpResponseUtil {
    private HttpResponseUtil() {
        throw new UnsupportedOperationException("Only static methods classe");
    }

    public static void prepareResponseWithCsv(String baseFileName, HttpServletResponse response) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.addHeader("Content-Disposition", "attachment; filename=" + excelExportFileName(baseFileName));
    }

    private static String excelExportFileName(String baseName) {//
        return "REGLISS-" + baseName + "-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmm").format(LocalDateTime.now()) + ".xls";
    }
}

