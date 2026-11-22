package com.bnpp.regliss.vo;

import com.bnpp.regliss.entity.ImportConfiguration;
import com.bnpp.regliss.entity.ImportFileType;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@Getter
public class SixAutomaticFeed implements AutomaticImportFeedBase {

    private final SixAutomaticFeedId feedId;
    private String xmlDataFileName;
    private boolean isAuto;
    private String originalFileName;
    private List<ImportLineErrorReport> errors = new ArrayList<>();


    public SixAutomaticFeed(SixAutomaticFeedId feedId, String filename) {
        this.feedId = feedId;
        fillFileName(filename);
    }
    public SixAutomaticFeed(SixAutomaticFeedId feedId, String filename, String originalFilename) {
        this.feedId = feedId;
        this.originalFileName = originalFilename;
        fillFileName(filename);
    }

    @Override
    public void addToErrors(List<ImportLineErrorReport> errors) {
        this.errors.addAll(errors);
    }

    @Override
    public void addToErrors(ImportLineErrorReport error) {
        this.errors.add(error);
    }

    @Override
    public final void fillFileName(String fileName) {
        String upperName = fileName.toUpperCase();
        if (upperName.endsWith(".XML")) {
            this.xmlDataFileName = fileName;
        } else {
            throw new IllegalArgumentException("Invalid file extension for Six Import. Current filename is: " + fileName);
        }
    }

    @Override
    public List<String> getAllFileNames() {
        return singletonList(xmlDataFileName);
    }

    @Override
    public List<String> getFileNamesToImport() {
        return singletonList(getXmlDataFileName());
    }

    @Override
    public boolean isFullImport() {
        return true;
    }

    @Override
    public boolean hasAllFilesFor(ImportConfiguration configuration) {
        return getMissingFiles(configuration).isEmpty();
    }

    @Override
    public List<AutoFeedFileType> getMissingFiles(ImportConfiguration configuration) {
        if (StringUtils.isEmpty(xmlDataFileName)) {
            return singletonList(AutoFeedFileType.XML_DATA_FILE);
        }
        return emptyList();
    }


    @Override
    public boolean isDj() {
        return false;
    }

    @Override
    public boolean isCustomAutomatic() {
        return false;
    }

    @Override
    public boolean isSixAutomatic() {
        return true;
    }
    @Override
    public void fillIsAuto(boolean flag) {
        isAuto = flag;
    }

    @Override
    public boolean isAuto() { return isAuto;}

    @Override
    public String toString() {
        return "SixAutomaticFeed{" +
                "feedId=" + feedId +
                ", xmlDataFileName='" + xmlDataFileName + '\'' +
                '}';
    }

}
