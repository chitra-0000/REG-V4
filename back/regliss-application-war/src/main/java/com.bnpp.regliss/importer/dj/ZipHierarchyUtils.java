package com.bnpp.regliss.importer.dj;

// [MISSING FROM SCREENSHOTS: lines 3-14 of the original file were not visible in the photos]

@Slf4j
@UtilityClass
public class ZipHierarchyUtils {

    private final String ZIP_PATH_CORE = "SANCTIONALERT/AGGREGATE/XML/";
    private static final String ZIP_PATH_WL_XML = "FACTIVA_PFA_FEED_XML";
    private static final String ZIP_PATH_WL_CSV = "FACTIVA_PFA_FEED_CSV";

    public boolean hasCorrectHierarchy(File zipFile, ImportFileType importFileType){
        if(ImportFileType.CUSTOM.equals(importFileType) || ImportFileType.DOW_JONES_WATCHLIST_EXTRA.equals(importFileType)){
            return hasCorrectCustomHierarchy(zipFile);
        }
        String filename = zipFile.getName();
        if(DJFilenameHelper.isAmeXml(filename) || DJFilenameHelper.isAmeCsv(filename)){
            return true;
        }
        if(DJFilenameHelper.isWatchlistXml(filename) || DJFilenameHelper.isWatchlistCsv(filename)){
            return hasHierarchy(zipFile, getHierarchyForWL(zipFile));
        }
        return hasHierarchy(zipFile, getHierarchyForCore(zipFile));
    }

    public String getCorrectHierarchy(File zipFile, ImportFileType importFileType){
        if(ImportFileType.CUSTOM.equals(importFileType)){
            return "*.XML";
        }
        String filename = zipFile.getName();
        if(DJFilenameHelper.isAmeXml(filename) || DJFilenameHelper.isAmeCsv(filename)){
            return "*";
        }
        if(DJFilenameHelper.isWatchlistXml(filename) || DJFilenameHelper.isWatchlistCsv(filename)){
            return getHierarchyForWL(zipFile);
        }
        return getHierarchyForCore(zipFile);
    }

    private boolean hasCorrectCustomHierarchy(File zipFile){
        boolean result = false;
        try(ZipFile zip = new ZipFile(zipFile)){
            long numberOfXMLFiles = zip.stream().filter(zipEntry -> zipEntry.getName().toUpperCase().endsWith(".XML"))
                    .count();
            if(numberOfXMLFiles==1){
                result =  true;
            }
            return result;
        } catch (IOException ioe) {
            log.debug(ioe.getMessage());
            return false;
        }
    }

    private  String getHierarchyForWL(File inputFile) {
        String fileNamePart = FilenameUtils.getBaseName(inputFile.getName());
        if (DJFilenameHelper.isWatchlistCsv(fileNamePart)) {
            return ZIP_PATH_WL_CSV.concat("/")
                    .concat(fileNamePart)
                    .concat(".CSV");
        } else {
            return ZIP_PATH_WL_XML.concat("/")
                    .concat(fileNamePart)
                    .concat(".XML");
        }
    }

    private  String getHierarchyForCore(File inputFile) {
        String fileNamePart = FilenameUtils.getBaseName(inputFile.getName());
        return ZIP_PATH_CORE.concat(fileNamePart).concat("/")
                .concat(fileNamePart)
                .concat(".XML");
    }

    private  boolean hasHierarchy(File inputFile, String path) {
        return getPathFromZip(inputFile)
                .map(path::equalsIgnoreCase)
                .orElse(false);
    }

    private  Optional<String> getPathFromZip(File inputFile){
        try(ZipFile zipFile = new ZipFile(inputFile)){
            return zipFile.stream().reduce((first, second) -> second)
                    .map(zipEntry -> Optional.of(zipEntry.getName()))
                    .orElse(empty());
        } catch (IOException ioe) {
            log.debug(ioe.getMessage());
            return empty();
        }
    }

}

