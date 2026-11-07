package com.bnpp.regliss.exporter.launcher;

import com.bnpp.regliss.entity.GeneratedFile;
import com.bnpp.regliss.entity.referential.FileExportMode;
import lombok.Getter;
import lombok.ToString;

import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@ToString
public class GenerateRegularFiles {
    @Getter
    private final Set<GeneratedFile> allFiles;

    public GenerateRegularFiles(Set<GeneratedFile> allFiles) {
        this.allFiles = Collections.unmodifiableSet(allFiles);
        if (getDataFiles().stream().anyMatch(GeneratedFile::isDJFormat)) {
            throw new IllegalArgumentException("No DJ files allowed: " + allFiles);
        }
    }

    public Optional<GeneratedFile> getDeliveryReportFile() { return getDeliveryReportFiles().findFirst(); }

    public Stream<GeneratedFile> getDeliveryReportFiles() {
        return allFiles.stream().filter(GeneratedFile::isDeliveryReport);
    }

    public Optional<GeneratedFile> getFircosoftZipFile() {
        return allFiles.stream().filter(GeneratedFile::isFircosoftZip).findFirst();
    }

    public Map<FileExportMode, List<GeneratedFile>> getDataFilesByExportMode() {
        return getDataFiles().stream().collect(groupingBy(GeneratedFile::getFileExportMode));
    }

    public Map<ExportQueryType, List<GeneratedFile>> queryTypesToFiles() {
        Map<ExportQueryType, List<GeneratedFile>> result = new HashMap<>();
        result.put(ExportQueryType.VERSION_REGULAR_FILES_EXPORT_QUERY, getVersionDataFiles());
        result.put(ExportQueryType.CONCATENATED_LIST_REGULAR_FILES_EXPORT_QUERY, getOfacFiles());
        getDeliveryReportFile().ifPresent(file -> result.put(ExportQueryType.DELIVERY_REPORT_EXPORT_QUERY, Collections.singletonList(file)));
        getFircosoftZipFile().ifPresent(file -> result.put(ExportQueryType.FIRCOSOFT_PV_DELIVERY_EXPORT_QUERY, Collections.singletonList(file)));
        return result;
    }

    private List<GeneratedFile> getVersionDataFiles() {
        return getDataFiles().stream()
                .filter(f -> f.getVersionOpt().isPresent())
                .collect(toList());
    }

    private List<GeneratedFile> getOfacFiles() {
        return getDataFiles().stream()
                .filter(f -> !f.getVersionOpt().isPresent())
                .collect(toList());
    }

    private List<GeneratedFile> getDataFiles() {
        return allFiles.stream()
                .filter(f -> !f.isDeliveryReport())
                .filter(f -> !f.isFircosoftZip())
                .collect(toList());
    }
}
