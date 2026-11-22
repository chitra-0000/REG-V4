package com.bnpp.regliss.utils;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.statistics.DjLogLivrasionStatistics;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
public class DjLogLivrasionUtil {

    private static final String TOTAL_TOKEN = "TOTAL NUMBER OF RECORDS";
    private static final String ADD_TOKEN = "NUMBER OF ADD";
    private static final String DELETE_TOKEN = "NUMBER OF DEL";
    private static final String MODIFIED_TOKEN = "NUMBER OF CHG";
    private static final String UPLOAD_DATE_TOKEN="FILE UPLOAD TIME IN GMT";

    private static void process(String line,DjLogLivrasionStatistics djLogLivrasionStatistics){
        if(line.contains(UPLOAD_DATE_TOKEN)){
            djLogLivrasionStatistics.setUploadDate(line.substring(line.indexOf(":-")+2).trim());
        }
        Pattern p = Pattern.compile("[^\\d]*(\\d+)[^\\d]*(\\d+)[^\\d]*(\\d+)");
        Matcher m = p.matcher(line);
        if(m.find()){
            if(line.contains(TOTAL_TOKEN)){
                if(line.contains("(")) {
                    djLogLivrasionStatistics.setTotalRecords(Integer.parseInt(m.group(1)));
                    djLogLivrasionStatistics.setTotalPersons(Integer.parseInt(m.group(2)));
                    djLogLivrasionStatistics.setTotalEntities(Integer.parseInt(m.group(3)));
                }else {
                    djLogLivrasionStatistics.setTotalRecords(Integer.parseInt(m.group(1)+m.group(2)+m.group(3)));
                }
            }

            if(line.contains(ADD_TOKEN)){
                djLogLivrasionStatistics.setAddRecords(Integer.parseInt(m.group(1)));
                djLogLivrasionStatistics.setAddPersons(Integer.parseInt(m.group(2)));
                djLogLivrasionStatistics.setAddEntities(Integer.parseInt(m.group(3)));
            }

            if(line.contains(MODIFIED_TOKEN)){
                djLogLivrasionStatistics.setModRecords(Integer.parseInt(m.group(1)));
                djLogLivrasionStatistics.setModPersons(Integer.parseInt(m.group(2)));
                djLogLivrasionStatistics.setModEntities(Integer.parseInt(m.group(3)));
            }

            if(line.contains(DELETE_TOKEN)){
                djLogLivrasionStatistics.setSupRecords(Integer.parseInt(m.group(1)));
                djLogLivrasionStatistics.setSupPersons(Integer.parseInt(m.group(2)));
                djLogLivrasionStatistics.setSupEntities(Integer.parseInt(m.group(3)));
            }

        }

    }

    public DjLogLivrasionStatistics extractRecordForToken(File logFile) throws ReglissException {
        DjLogLivrasionStatistics djLogLivrasionStatistics=new DjLogLivrasionStatistics();
        try (Stream<String> lines = Files.lines(logFile.toPath())) {
            lines.filter(StringUtils::isNotBlank).forEach(line->process(line,djLogLivrasionStatistics));
        } catch (IOException e) {
            throw new ReglissException(e);
        }
        return djLogLivrasionStatistics;
    }
}
