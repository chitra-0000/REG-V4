package com.bnpp.regliss.six.exporter.service;

import com.bnpp.regliss.entity.ImportedFile;
import com.bnpp.regliss.entity.batch.BatchJobExecution;
import com.bnpp.regliss.entity.six.entity.SixFilters;
import com.bnpp.regliss.entity.six.entity.SixSubFilters;
import com.bnpp.regliss.repository.ImportedFileRepository;
import com.bnpp.regliss.repository.VersionRepository;
import com.bnpp.regliss.repository.batch.BatchJobExecutionRepository;
import com.bnpp.regliss.repository.six.SixFiltersRepository;
import com.bnpp.regliss.six.exporter.generated.ListType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class SixXmlGenerationService {

    private static final String DEFAULT_HOLDING_FOLDER = "six-export-holding";
    private static final String XML_EXTENSION = ".xml";
    private static final String TMP_EXTENSION = ".tmp";
    private static final Pattern UNSAFE_FOLDER_CHARS = Pattern.compile("[^A-Za-z0-9_.-]");

    @Value("${automatic.import.IN.directory}")
    private File baseTempFolder;

    @Value("${automatic.import.xsd.CUSTOM_AUTOMATIC}")
    public File customAutomaticXsd;

    @Value("${six.export.holding.directory:}")
    private String holdingDirectory;

    @Autowired
    private ImportedFileRepository importedFileRepository;

    @Autowired
    private BatchJobExecutionRepository batchJobExecutionRepository;

    @Autowired
    private VersionRepository versionRepository;

    @Autowired
    private SixFiltersRepository sixFiltersRepository;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    public static final Pattern date = Pattern.compile("_(\\d{8}_\\d{6})$$");

    public Path createHeldFileOrThrow(ListType listType, Set<Long> batchJobExecutionIds, String delivery, String listRef,
                                      Long instrumentVersionId) {
        String fileName = converterFileName(listType, batchJobExecutionIds, instrumentVersionId);
        Path folder = heldFolder(delivery, listRef);
        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            throw new IllegalStateException("Holding folder " + folder + " could not be created: " + e.getMessage(), e);
        }
        writeValidatedFile(listType, folder, fileName, activeFilters(listRef).orElse(null));   // null: "filters not available"
        log.info("File {} held at {} until its delivery {} is complete", fileName, folder, delivery);
        return folder.resolve(fileName);
    }

    public List<String> releaseHeldFiles(String delivery, String listRef) throws IOException {
        Path folder = heldFolder(delivery, listRef);
        if (!Files.isDirectory(folder)) {
            return new ArrayList<>();
        }
        List<Path> held;
        try (Stream<Path> files = Files.list(folder)) {
            held = files.filter(f -> f.getFileName().toString().endsWith(XML_EXTENSION)).sorted().collect(Collectors.toList());
        }
        Path inDir = djInFolder();
        List<String> moved = new ArrayList<>();
        for (Path file : held) {
            String name = file.getFileName().toString();
            moveIntoDjIn(file, inDir, name);
            moved.add(name);
            log.info("File {} moved from {} to {}", name, folder, inDir);
        }
        deleteIfEmpty(folder);
        deleteIfEmpty(folder.getParent());
        return moved;
    }

    Path heldFolder(String delivery, String listRef) {
        Path root = holdingRoot();
        Path folder = root.resolve(safeName(delivery)).resolve(safeName(listRef)).normalize();
        if (!folder.startsWith(root)) {
            throw new IllegalArgumentException("Invalid holding folder for delivery " + delivery + ", list " + listRef);
        }
        return folder;
    }

    Path holdingRoot() {
        Path inDir = djInFolder();
        Path root;
        if (holdingDirectory == null || holdingDirectory.trim().isEmpty()) {
            Path parent = inDir.getParent();
            if (parent == null) {
                throw new IllegalStateException("No folder above the DJ IN folder " + inDir + ": set six.export.holding.directory");
            }
            root = parent.resolve(DEFAULT_HOLDING_FOLDER);
        } else {
            root = Paths.get(holdingDirectory.trim()).toAbsolutePath().normalize();
        }
        if (root.startsWith(inDir)) {
            throw new IllegalStateException("six.export.holding.directory " + root + " must not be inside the DJ IN folder " + inDir);
        }
        return root;
    }

    private Path djInFolder() {
        return baseTempFolder.toPath().toAbsolutePath().normalize();
    }

    private static String safeName(String value) {
        String name = value == null ? "" : UNSAFE_FOLDER_CHARS.matcher(value.trim()).replaceAll("_");
        return name.isEmpty() ? "_" : name;
    }

    private static void moveIntoDjIn(Path held, Path inDir, String name) throws IOException {
        Path target = inDir.resolve(name);
        try {
            Files.move(held, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            log.warn("Holding folder and DJ IN folder are on different mounts: {} copied", name);
            log.debug("Atomic move of {} not supported", name, e);
            Path temp = Files.createTempFile(inDir, name + "_", TMP_EXTENSION);
            try {
                Files.copy(held, temp, StandardCopyOption.REPLACE_EXISTING);
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                Files.delete(held);
            } finally {
                Files.deleteIfExists(temp);
            }
        }
    }

    private static void deleteIfEmpty(Path folder) {
        try {
            if (folder != null) {
                Files.deleteIfExists(folder);
            }
        } catch (DirectoryNotEmptyException e) {
            log.debug("Holding folder {} kept: other files are still held there", folder, e);
        } catch (IOException e) {
            log.warn("Holding folder {} could not be removed: {}", folder, e.getMessage());
        }
    }

    public List<HeldFile> heldFiles() throws IOException {
        Path root = holdingRoot();
        List<HeldFile> held = new ArrayList<>();
        for (Path group : subFolders(root)) {
            for (Path list : subFolders(group)) {
                for (Path file : entries(list)) {
                    if (Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) && file.getFileName().toString().endsWith(XML_EXTENSION)) {
                        LocalDateTime modified = LocalDateTime.ofInstant(
                                Files.getLastModifiedTime(file, LinkOption.NOFOLLOW_LINKS).toInstant(), ZoneId.systemDefault());
                        held.add(new HeldFile(group.getFileName().toString(), list.getFileName().toString(), file, modified));
                    }
                }
            }
        }
        return held;
    }

    public String deliverHeldFile(HeldFile held) throws IOException {
        Path file = checkedHeldPath(held);
        String name = file.getFileName().toString();
        moveIntoDjIn(file, djInFolder(), name);
        log.info("File {} moved from {} to {}", name, file.getParent(), djInFolder());
        return name;
    }

    /** Deletes one left-over held file (older than what DJ already received for its list). */
    public void discardHeldFile(HeldFile held) throws IOException {
        Files.deleteIfExists(checkedHeldPath(held));
    }

    public int removeTempFilesAndEmptyFolders(long tmpAgeMinutes) throws IOException {
        Path root = holdingRoot();
        long limit = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(tmpAgeMinutes);
        int removed = 0;
        for (Path group : subFolders(root)) {
            for (Path list : subFolders(group)) {
                for (Path file : entries(list)) {
                    if (Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) && file.getFileName().toString().endsWith(TMP_EXTENSION)
                            && Files.getLastModifiedTime(file, LinkOption.NOFOLLOW_LINKS).toMillis() < limit) {
                        Files.deleteIfExists(file);
                        removed++;
                    }
                }
                deleteIfEmpty(list);
            }
            deleteIfEmpty(group);
        }
        return removed;
    }

    public static String holdingFolderName(String value) {
        return safeName(value);
    }

    private Path checkedHeldPath(HeldFile held) {
        Path root = holdingRoot();
        Path file = held.getPath().toAbsolutePath().normalize();
        if (!file.startsWith(root) || !file.getFileName().toString().endsWith(XML_EXTENSION)) {
            throw new IllegalArgumentException("Not a held CONVERTER file: " + file);
        }
        return file;
    }

    private static List<Path> subFolders(Path folder) throws IOException {
        List<Path> folders = new ArrayList<>();
        for (Path entry : entries(folder)) {
            if (Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS)) {
                folders.add(entry);
            }
        }
        return folders;
    }

    private static List<Path> entries(Path folder) throws IOException {
        if (!Files.isDirectory(folder, LinkOption.NOFOLLOW_LINKS)) {
            return new ArrayList<>();
        }
        try (Stream<Path> entries = Files.list(folder)) {
            return entries.sorted().collect(Collectors.toList());
        }
    }

    public static final class HeldFile {
        private final String group;
        private final String listFolder;
        private final Path path;
        private final LocalDateTime lastModified;

        HeldFile(String group, String listFolder, Path path, LocalDateTime lastModified) {
            this.group = group;
            this.listFolder = listFolder;
            this.path = path;
            this.lastModified = lastModified;
        }

        public String getGroup() { return group; }
        public String getListFolder() { return listFolder; }
        public Path getPath() { return path; }
        public LocalDateTime getLastModified() { return lastModified; }

        @Override
        public String toString() {
            return group + "/" + listFolder + "/" + path.getFileName();
        }
    }

    private String converterFileName(ListType listType, Set<Long> batchJobExecutionIds, Long instrumentVersionId) {
        List<BatchJobExecution> batchJobExecutionList = batchJobExecutionRepository.findAllById(batchJobExecutionIds);
        boolean regenerationFilter = batchJobExecutionList.stream()
                .map(BatchJobExecution::getJobParams).allMatch(params -> params != null && params.contains("REGENERATION"));
        String dateTimeToFilledInFileName = regenerationFilter ? LocalDateTime.now().format(FMT) : buildApplicationDateFromFileName(instrumentVersionId);
        return "CONVERTER-" + listType.getLabel() + "_" + listType.getSourceVersion() + "_" + dateTimeToFilledInFileName + XML_EXTENSION;
    }

    private void writeValidatedFile(ListType listType, Path folder, String fileName, List<SixFilters> filters) {
        try {
            writeFile(listType, folder, fileName, filters);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("CONVERTER file " + fileName + " not created: " + e.getMessage(), e);
        }
    }

    private void writeFile(ListType listType, Path folder, String fileName, List<SixFilters> filters)
            throws IOException, SAXException, JAXBException {
        Path target = folder.resolve(fileName);
        Path temp = Files.createTempFile(folder, fileName + "_", TMP_EXTENSION);
        try {
            try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(temp, StandardOpenOption.WRITE))) {
                ExportFileWriter.writeFile(listType, out, filters);
            }
            validateXsdSchema(temp.toFile());
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            log.info("File {} stored at {}", fileName, target);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private Optional<List<SixFilters>> activeFilters(String listRef) {
        try {
            List<SixFilters> filters = sixFiltersRepository.getFiltersByListRefAndActiveTrueAndDeletedFalse(listRef);
            if (filters == null) {
                return Optional.of(Collections.emptyList());
            }
            filters.forEach(f -> f.getActiveSixSubfilters().forEach(SixSubFilters::getActiveSixSubORfilters));   // load now
            return Optional.of(filters);
        } catch (RuntimeException e) {
            log.warn("Filters of list {} not read for the footer of its CONVERTER file: {}", listRef, e.getMessage());
            return Optional.empty();
        }
    }

    private void validateXsdSchema(File xml) throws SAXException, IOException {
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        sf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Schema schema = sf.newSchema(customAutomaticXsd);
        Validator validator = schema.newValidator();
        validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        validator.validate(new StreamSource(xml));
    }

    private String buildApplicationDateFromFileName(Long instrumentVersionId) {
        String match = "";
        Long versionId = instrumentVersionId != null ? instrumentVersionId : versionRepository.getLatestVersionOfInstruments().getId();
        ImportedFile importedFile = importedFileRepository.findByVersionId(versionId);
        String nameWithoutExtension = FilenameUtils.getBaseName(importedFile.getOriginalFileName()).toUpperCase();
        Matcher m = date.matcher(nameWithoutExtension);
        if(m.find())
        {
            match= m.group(1);
        }
        return match;
    }
}
