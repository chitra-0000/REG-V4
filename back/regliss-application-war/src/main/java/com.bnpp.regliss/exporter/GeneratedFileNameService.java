package com.bnpp.regliss.exporter;

import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.entity.referential.FileExportExtension;
import com.bnpp.regliss.entity.referential.FileExportFormat;
import com.bnpp.regliss.entity.referential.FileExportMode;
import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import com.bnpp.regliss.service.ReglissRequestContext;
import com.bnpp.regliss.utils.DJFilenameHelper;
import com.bnpp.regliss.utils.FeedIdHelper;
import com.bnpp.regliss.utils.FormatDateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class GeneratedFileNameService {
    private static final String FIRCOSOFT_FILE_NAME_PREFIX = "RESHUIS1";
    private static final String FIRCOSOFT_PV_XML_VERSION = "V4";
    private static final String FIRCOSOFT_WATCHLIST_PV_FILE_NAME_PREFIX = "decompte";
    private static final String WATCHLIST_LOG_PREFIX = "Log_PEP";
    private static final String DATE_TIME_FORMAT_WITH_LINE = "ddMMyyy-HHmmss";

    @Value("${export.generic_full_name}")
    private String genericFullName;

    @Value("${export.regliss_full_name}")
    private String reglissFullName;
    @Value("${export.regliss_add_mod_name}")
    private String reglissAddModName;
    @Value("${export.regliss_add_name}")
    private String reglissAddName;
    @Value("${export.regliss_mod_name}")
    private String reglissModName;
    @Value("${export.regliss_supp_name}")
    private String reglissSuppName;

    @Value("${export.generic_add_name}")
    private String genericAddName;
    @Value("${export.generic_mod_name}")
    private String genericModName;
    @Value("${export.generic_supp_name}")
    private String genericSuppName;

    @Value("${export.vigilance_full_name}")
    private String vigilanceFullName;

    @Value("${export.vigilance_add_mod_name}")
    private String vigilanceAddModName;
    @Value("${export.vigilance_add_name}")
    private String vigilanceAddName;
    @Value("${export.vigilance_mod_name}")
    private String vigilanceModName;
    @Value("${export.vigilance_supp_name}")
    private String vigilanceSuppName;

    @Value("${export.safewatch_full_name}")
    private String safewatchFullName;

    @Value("${export.safewatch_add_mod_name}")
    private String safewatchAddModName;
    @Value("${export.safewatch_add_name}")
    private String safewatchAddName;
    @Value("${export.safewatch_mod_name}")
    private String safewatchModName;
    @Value("${export.safewatch_supp_name}")
    private String safewatchSuppName;

    @Value("${export.fircosoft_full_name}")
    private String fircosoftFullName;

    @Value("${export.core_full_name}")
    private String coreFullName;

    @Value("${export.core_delta_name}")
    private String coreDeltaName;

    @Value("${export.ofac_regliss_full_name}")
    private String ofacReglissFullName;

    @Value("${export.ofac_vigilance_full_name}")
    private String ofacVigilanceFullName;

    @Value("${export.ofac_generic_full_name}")
    private String ofacGenericFullName;


    @Value("${export.add.mod.sup.excel.name}")
    private String addModSupExcelName;

    @Autowired
    private ReglissRequestContext requestContext;

    public String getFilename(GeneratedFileFormat format, Version version, ConcatenatedList concatenatedList) {
        if(concatenatedList != null){
            return getFilenamePrefixForConcatenatedList(format, concatenatedList)
                    + "";
        }
        return getFilenamePrefix(format, version)
                + getFilenameSuffix(format, version);
    }

    public String getAddModSupExcelFilename(Version version) {
        return addModSupExcelName + "_" + version.getListAndVersionReference();
    }

    public String getFircosoftLogFilename() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_WITH_LINE);
        return FIRCOSOFT_FILE_NAME_PREFIX + "_" + LocalDateTime.now().format(formatter);
    }

    public String getFircosoftXmlPVFilename() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_WITH_LINE);
        return FIRCOSOFT_WATCHLIST_PV_FILE_NAME_PREFIX + "_" + LocalDateTime.now().format(formatter) + "_" + FIRCOSOFT_PV_XML_VERSION;
    }

    public String getFircosoftZipPV(String fileName) { return fileName + "_" + FIRCOSOFT_PV_XML_VERSION; }

    public String getAddModSupAuditTrailFilename(Version version) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FormatDateUtil.DATE_TIME_FORMAT_WITH_LINE_AUDIT_TRAIL);
        if (version.isVerified()) {
            return "Verification_" + version.getList().getReference() + "."
                    + version.getVersionNumber() + "_" + version.getVerificationDate().format(formatter);
        } else {
            return "Verification_" + version.getList().getReference() + "." + version.getVersionNumber() + "_not_verified_"
                    + LocalDateTime.now().format(formatter);
        }
    }

    private String getFilenamePrefixForConcatenatedList(GeneratedFileFormat format, ConcatenatedList concatenatedList){
        FileExportFormat exportFormat = format.getFileFormat();
        return getFilenamePrefixForConcatenatedList(concatenatedList, exportFormat);
    }

    private String getFilenamePrefix(GeneratedFileFormat format, Version version) {
        FileExportFormat exportFormat = format.getFileFormat();
        switch (format.getFileExportMode()) {
            case FULL:
                return getFilenamePrefixForFULL(version, exportFormat, format.getFileExtension());
            case ADD:
                return getFilenamePrefixForAdd(exportFormat);
            case MOD:
                return getFilenamePrefixForMod(exportFormat);
            case SUPP:
                return getFilenamePrefixForSupp(exportFormat);
            case SYNTHESIS_OF_ADD:
                return getFilenamePrefixForSynthesisOfAdd(exportFormat);
            case SYNTHESIS_OF_MOD:
                return getFilenamePrefixForSynthesisOfMod(exportFormat);
            case SYNTHESIS_OF_SUPP:
                return getFilenamePrefixForSynthesisOfSupp(exportFormat);
            case ADD_MOD:
            default:
                return getFilenamePrefixForDELTA(exportFormat);
        }
    }

    private String getFilenamePrefixForAdd(FileExportFormat exportFormat) {
        if (exportFormat.equals(FileExportFormat.REGLISS)) {
            return reglissAddName + "_";
        } else if (exportFormat.equals(FileExportFormat.VIGILANCE)) {
            return vigilanceAddName + "_";
        } else if (exportFormat.equals(FileExportFormat.SAFEWATCH)) {
            return safewatchAddName + "_";
        }
        return "";
    }

    private String getFilenamePrefixForSynthesisOfAdd(FileExportFormat exportFormat) {
        if (exportFormat.equals(FileExportFormat.GENERIC)) {
            return genericAddName + "_" ;
        }
        return "";
    }

    private String getFilenamePrefixForMod(FileExportFormat exportFormat) {
        if (exportFormat.equals(FileExportFormat.REGLISS)) {
            return reglissModName + "_";
        } else if (exportFormat.equals(FileExportFormat.VIGILANCE)) {
            return vigilanceModName + "_";
        } else if (exportFormat.equals(FileExportFormat.SAFEWATCH)) {
            return safewatchModName + "_";
        }
        return "";
    }

    private String getFilenamePrefixForSynthesisOfMod(FileExportFormat exportFormat) {
        if (exportFormat.equals(FileExportFormat.GENERIC)) {
            return genericModName + "_";
        }
        return "";
    }

    private String getFilenamePrefixForAddMod(FileExportFormat exportFormat) {
        if (exportFormat.equals(FileExportFormat.REGLISS)) {
            return reglissAddModName + "_";
        } else if (exportFormat.equals(FileExportFormat.VIGILANCE)) {
            return vigilanceAddModName + "_";
        } else if (exportFormat.equals(FileExportFormat.SAFEWATCH)) {
            return safewatchAddModName + "_";
        }
        return "";
    }

    private String getFilenamePrefixForSupp(FileExportFormat exportFormat) {
        if (exportFormat.equals(FileExportFormat.REGLISS)) {
            return reglissSuppName + "_";
        } else if (exportFormat.equals(FileExportFormat.VIGILANCE)) {
            return vigilanceSuppName + "_";
        } else if (exportFormat.equals(FileExportFormat.SAFEWATCH)) {
            return safewatchSuppName + "_";
        }
        return "";
    }

    private String getFilenamePrefixForSynthesisOfSupp(FileExportFormat exportFormat) {
        if (exportFormat.equals(FileExportFormat.GENERIC)) {
            return genericSuppName + "_";
        }
        return "";
    }

    private String getFilenamePrefixForDELTA(FileExportFormat exportFormat) {
        if (exportFormat.equals(FileExportFormat.DJ_CORE)) {
            return coreDeltaName + "_";
        }
        return getFilenamePrefixForAddMod(exportFormat);
    }

    private String getFilenamePrefixForConcatenatedList(ConcatenatedList concatenatedList,
                                             FileExportFormat exportFormat) {
        if (ReferentialBusinessConcept.GLOBAL_PLUS_OFAC.equals(concatenatedList.getBusinessConcept())) {
            switch (exportFormat) {
                case REGLISS:
                    return ofacReglissFullName;
                case VIGILANCE:
                    return ofacVigilanceFullName;
                case GENERIC:
                    return ofacGenericFullName;
                default:
                    throw new IllegalArgumentException("Unsupported OFAC format: " + exportFormat);
            }
        }

            switch (exportFormat) {
                case REGLISS:
                    return concatenatedList.getLabel() + "regliss";
                case VIGILANCE:
                    return concatenatedList.getLabel() + "vigilence";
                case GENERIC:
                    return concatenatedList.getLabel() + "generic";
                case SAFEWATCH:
                    return concatenatedList.getLabel() + "safewatch";
                case FIRCOSOFT:
                    return concatenatedList.getLabel() + "fircosoft";
                default:
                    throw new IllegalArgumentException("Unsupported Concatenated List format: " + exportFormat);
            }

    }

    private String getFilenamePrefixForFULL(Version version, FileExportFormat exportFormat,
                                            FileExportExtension extension) {
        if (version == null) {
            switch (exportFormat) {
                case REGLISS:
                    return ofacReglissFullName;
                case VIGILANCE:
                    return ofacVigilanceFullName;
                case GENERIC:
                    return ofacGenericFullName;
                default:
                    throw new IllegalArgumentException("Unsupported OFAC format: " + exportFormat);
            }
        }

        switch (exportFormat) {
            case GENERIC:
                return getGenericExtensionPrefix(version);
            case REGLISS:
                return reglissFullName + "_";
            case VIGILANCE:
                return vigilanceFullName + "_";
            case SAFEWATCH:
                return safewatchFullName + "_";
            case DJ_CORE:
                return coreFullName + "_";
            case FIRCOSOFT:
                return getFircosoftExtensionPrefix(extension, version);
            default:
                return "";
        }

    }

    private String getGenericExtensionPrefix(Version version) {
        ReglissList list = version.getList();
        String listShortLabel = list.getShortName();
        return genericFullName + "_" + listShortLabel + ".";
    }

    private String getFircosoftExtensionPrefix(FileExportExtension extension, Version version) {
        ReglissList list = version.getList();
        String listName = list.getShortName();
        String listRef = String.format("%01d", Long.valueOf(list.getReference()));
        switch (extension) {
            case txt:
                return "list" + "_" + fircosoftFullName + "_" + listName + ".";
            case alt:
                return "aka" + "_" + fircosoftFullName + "_" + listName + ".";
            case add:
                return "address" + "_" + fircosoftFullName + "_" + listName + ".";
            default:
                return listName + "_" + listRef + "_";
        }
    }

    private String getFilenameSuffix(GeneratedFileFormat format, Version version) {
        if (version == null) {
            return "";
        }

        if (format.getFileFormat() == FileExportFormat.DJ_WATCHLIST ) {
            return this.createWatchlistSuffix(format);
        }
        if (format.getFilter() != null) {
            String filterName = format.getFilter().getFilterName();
            if (filterName.contains("*")) {
                String fileDate = FeedIdHelper.djFeedIdFromFilename(version.getFileName()).getDateStamp();
                return filterName.replace("*", fileDate);
            }
            return filterName;
        }
        if (format.getFileFormat() == FileExportFormat.FIRCOSOFT &&
                format.getFileExtension() == FileExportExtension.zip) {
            return version.getVersionReference() + "_"
                    + requestContext.getRequestTime().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        }
        if (format.getFileFormat() == FileExportFormat.GENERIC &&
                (format.getFileExportMode()== FileExportMode.SYNTHESIS_OF_ADD ||
                 format.getFileExportMode()== FileExportMode.SYNTHESIS_OF_MOD ||
                 format.getFileExportMode()== FileExportMode.SYNTHESIS_OF_SUPP)){
            return version.getList().getShortName() + "."
                    + version.getListAndVersionReference();
        }

        return version.getListAndVersionReference();
    }

    private String createWatchlistSuffix(GeneratedFileFormat format){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String fileDate = LocalDateTime.now().format(formatter);
        String filterName = "";
        String prefixZip = "";
        DJExportFilter djExportFilter = format.getFilter();
        if (djExportFilter != null) {
            filterName = djExportFilter.getFilterName().replace("*", "");
            prefixZip = djExportFilter.getType().equals(FileExportMode.FULL)? "full_" : "delta_";
        }

        return prefixZip + fileDate + "_" + "*_" + filterName;
    }

    public String getWatchlistLogFilename() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_WITH_LINE);
        return WATCHLIST_LOG_PREFIX + "_" + LocalDateTime.now().format(formatter);
    }

    public String getWatchlistXmlPVFilename() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_WITH_LINE);
        return FIRCOSOFT_WATCHLIST_PV_FILE_NAME_PREFIX + "_" + LocalDateTime.now().format(formatter);
    }

    public String getWatchlistZip(String fileName) { return fileName; }

    public String getWatchlistXmlZip(String filename,Version version) {
        String fileDate = FeedIdHelper.djFeedIdFromFilename(version.getFileName()).getDateStamp();
        String suffix = filename.contains("full") ? "_F" : "_I";
        return DJFilenameHelper.getWlAmeXmlPrefix(ImportFileType.DOW_JONES_WATCHLIST) + "_" + fileDate + suffix;
    }
}

