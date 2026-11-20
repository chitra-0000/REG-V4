package com.bnpp.regliss.scheduler;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.entity.ImportedFile;
import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.importer.UnicodeBOMInputStream;
import com.bnpp.regliss.importer.dj.ImportService;
import com.bnpp.regliss.importer.manual.ManualImportFileRepository;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.vo.AutomaticImportFeedBase;
import com.bnpp.regliss.vo.CustomAutomaticFeed;
import com.bnpp.regliss.vo.CustomAutomaticFeedId;
import com.bnpp.regliss.vo.DJFeed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.function.Supplier;

import static org.apache.commons.lang3.StringUtils.isEmpty;

@Slf4j
@Service
@ReglissBatchProfile
public abstract class FeedImporter {


    @Autowired
    private FileRepository fileRepo;

    @Autowired
    private ManualImportFileRepository manualImportFileRepository;

    @Autowired
    private AutomaticImportFileRepository automaticImportFileRepository;

    @Autowired
    private AutomaticSixImportFileRepository automaticSixImportFileRepository;

    protected void insertXmlValuesIntoCAFeed(CustomAutomaticFeed feed) {
        Supplier<UnicodeBOMInputStream> xmlInputStreamSupplier = openFromZipIfNecessary(feed, false);
        CustomAutomaticFeedId feedId = feed.getFeedId();

        try {
            CustomAutomaticXmlHandler caXmlHandler = CustomAutomaticXmlHandler.getXmlDataFromCustomAutomaticFile(xmlInputStreamSupplier);

            String sourceVersionAsString = caXmlHandler.getSourceVersion();
            feedId.setSourceVersion(isEmpty(sourceVersionAsString) ? null : Long.parseLong(sourceVersionAsString));
            feedId.setApplicationDate(caXmlHandler.getApplicationDate());
            feedId.setJuridictionBase(caXmlHandler.getJuridictionBase());
        } catch (IOException e) {
            throw new ReglissException(e, ReglissException.ErrorCode.IMPORT_FILE_PARSE_ERROR);
        }
    }


    protected Supplier<UnicodeBOMInputStream> openFromZipIfNecessary(AutomaticImportFeedBase feed, boolean isCsvFile){
        if(feed.isAuto()){
            if(isCsvFile){
                return automaticImportFileRepository.openFromZipIfNecessary(((DJFeed)feed).getCsvDataFileName());
            } else if (feed.isSixAutomatic()){
                return automaticSixImportFileRepository.openFromZipIfNecessary(feed.getXmlDataFileName());
            } else {
                return automaticImportFileRepository.openFromZipIfNecessary(feed.getXmlDataFileName());
            }
        }else{
            return manualImportFileRepository.openFromZipIfNecessary(feed.getXmlDataFileName());
        }
    }

}

