package com.bnpp.regliss.usersync;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@ReglissWebProfile
@Service
@Slf4j
public class CsvWriter {

    @Autowired
    private EmailService emailService;

    @Value("${temp.file.folder}")
    private File baseTempFolder;

    static final String REFOG_USER_SYNC = "refogUserSync_";
    static final String XLSX_EXTENSION = ".xlsx";
    static final String SEPARATOR = "/";
    private static final SecureRandom random = new SecureRandom();

    public void writeExcelSheet(Map<Integer, String> excelData, int deactivatedUserCount, int modifiedUserCount) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet spreadsheet = workbook.createSheet("User Data");
            XSSFRow row;
            int rowid = 0;
            for (Map.Entry<Integer, String> data : excelData.entrySet()) {
                row = spreadsheet.createRow(rowid++);
                String objectArr = data.getValue();
                int cellId = 0;
                String[] arr = objectArr.split(";");
                for (int i = 0; i < arr.length; i++) {
                    spreadsheet.setColumnWidth(cellId, 8000);
                    Cell cell = row.createCell(cellId++);
                    if (rowid == 1) {
                        XSSFFont font = workbook.createFont();
                        font.setBold(true);
                        XSSFCellStyle cellStyle = workbook.createCellStyle();
                        cellStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
                        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                        cellStyle.setFont(font);
                        cell.setCellStyle(cellStyle);
                        spreadsheet.setAutoFilter(new CellRangeAddress(0, 0, 0, arr.length - 1));
                        row.setHeightInPoints(50);
                    }
                    row.setHeightInPoints(15);
                    cell.setCellValue(arr[i]);
                }
            }
            log.info("Creating tempCsvWorkFile");
            File tempFile = new File(baseTempFolder + SEPARATOR + REFOG_USER_SYNC + random + XLSX_EXTENSION);
            tempFile.createNewFile();
            log.info("TempCsvWorkFile : {}", tempFile.getAbsolutePath());

            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                workbook.write(out);
                log.info("Preparing to send mail");
                sendMail(tempFile, deactivatedUserCount, modifiedUserCount);
                log.info("mail sent");
            }
            tempFile.delete();
        } catch (IOException e) {
            log.info("Error message : {}", e.getMessage());
        }
    }

    public void sendMail(File file, int deactivatedUserCount, int modifiedUserCount) {
        String date = String.valueOf(LocalDateTime.now());
        emailService.sendEmailForUserSyncWithRefog(file, deactivatedUserCount, modifiedUserCount, date);
    }
}

