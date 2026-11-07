package com.bnpp.regliss.exporter.launcher;

import com.bnpp.regliss.entity.GeneratedFile;
import com.bnpp.regliss.entity.referential.FileExportMode;
import com.bnpp.regliss.exporter.launcher.filtering.watchlistame.CounterKeys;
import lombok.Getter;
import lombok.ToString;

import java.util.*;

import static com.bnpp.regliss.entity.referential.FileExportMode.FULL;
import static java.util.stream.Collectors.*;

@ToString
public class GenerateDJFiles {
    @Getter
    private final Set<GeneratedFile> allFiles;
    private final boolean hasPreviousVersion;


    public GenerateDJFiles(Set<GeneratedFile> allFiles, boolean hasPreviousVersion) {
        this.allFiles = Collections.unmodifiableSet(allFiles);
        if (allFiles.stream().anyMatch(GeneratedFile::isRegularFormat)) {
            throw new IllegalArgumentException("Only DJ files allowed: " + allFiles);
        }
        this.hasPreviousVersion = hasPreviousVersion;
    }

    public Map<FileExportMode, List<GeneratedFile>> getDjCoreFilesByExportMode() {
        return getDjCoreFiles().stream().collect(groupingBy(GeneratedFile::getFileExportMode));
    }

    public Map<FileExportMode, List<GeneratedFile>> getWatchlistAmeFilesNotExportingFullAllByExportMode() {
        return getWatchlistAmeFilesNotExportingFullAll().stream()
                .collect(groupingBy(GeneratedFile::getFileExportMode));
    }

    public List<GeneratedFile> getWatchlistAmeFilesNotExportingFullAll() {
        List<GeneratedFile> watchlistAmeFullAllFiles = getWatchlistAmeFilesFullExportingAll();
        return getDjWatchlistAmeFiles().stream()
                .filter(file -> !watchlistAmeFullAllFiles.contains(file) && !file.isWatchlistZip())
                .collect(toList());
    }

    public List<GeneratedFile> getWatchlistAmeFilesFullExportingAll() {
        return getDjWatchlistAmeFiles().stream()
                .filter(file -> file.isAFullAllFilter(hasPreviousVersion) && !file.isWatchlistZip())
                .collect(toList());
    }

    public List<GeneratedFile> getWatchlistAmeFilesNotFullExportingAll() {
        return getDjWatchlistAmeFiles().stream()
                .filter(file -> file.getGeneratedFileFormat().getFilter().exportsAll() && file.getFileExportMode()
                        .getActualExportMode(hasPreviousVersion) != FULL)
                .collect(toList());
    }

    public List<GeneratedFile> getWatchlistAmeFilesNotExportingAll() {
        return getDjWatchlistAmeFiles().stream()
                .filter(file -> !file.getGeneratedFileFormat().getFilter().exportsAll())
                .collect(toList());
    }

    public boolean hasPreviousVersion() { return hasPreviousVersion; }

    private List<GeneratedFile> getDjCoreFiles() {
        return allFiles.stream()
                .filter(GeneratedFile::isDJCoreFormat)
                .collect(toList());
    }

    public Map<ExportQueryType, List<GeneratedFile>> queryTypesToFiles() {
        Map<ExportQueryType, List<GeneratedFile>> result = new HashMap<>();
        result.put(ExportQueryType.CORE_EXPORT_QUERY, getDjCoreFiles());
        result.put(ExportQueryType.WATCHLIST_AME_FULL_ALL_EXPORT_QUERY, getWatchlistAmeFilesFullExportingAll());
        result.put(ExportQueryType.WATCHLIST_AME_NOT_FULL_ALL_EXPORT_QUERY, getWatchlistAmeFilesNotExportingFullAll());

        return result;
    }

    public Set<GeneratedFile> getDjWatchlistAmeFiles() {
        return allFiles.stream()
                .filter(file -> !file.isWatchlistZip() &&(file.isDJWatchlistFormat() || file.isDJAmeFormat()))
                .collect(toSet());
    }

    public Set<GeneratedFile> getWatchlistZipFiles() {
        return allFiles.stream().filter(GeneratedFile::isWatchlistZip).collect(toSet());
    }



}
