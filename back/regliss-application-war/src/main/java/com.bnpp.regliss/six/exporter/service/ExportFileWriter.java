package com.bnpp.regliss.six.exporter.service;

import com.bnpp.regliss.entity.six.entity.SixFilters;
import com.bnpp.regliss.entity.six.entity.SixSubFilters;
import com.bnpp.regliss.entity.six.entity.SixSubORFilters;
import com.bnpp.regliss.six.exporter.generated.ListType;
import lombok.extern.slf4j.Slf4j;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
public final class ExportFileWriter {

    private static final DateTimeFormatter FOOTER_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String FOOTER_LINE = "<!-- ####################################################################### -->\n";
    private static final List<String> FILE_TYPE_ORDER = Arrays.asList("instrument file", "structured file", "options file",
            "data extractor 1", "data extractor 2");
    private static final Comparator<SixFilters> FILTER_ORDER = Comparator.comparingInt((SixFilters f) -> fileTypeRank(f.getFileType()))
            .thenComparing(f -> nullToEmpty(f.getFilterName()), String.CASE_INSENSITIVE_ORDER);

    private static final String XML_OBJ_REQUIRED = "xmlObj must not be null";

    private static final AtomicReference<JAXBContext> LIST_TYPE_CONTEXT = new AtomicReference<>();

    private ExportFileWriter() {
        // static helpers only
    }

    public static InputStream createFile(
            ListType xmlObj,
            String filterUser,
            String filterSource,
            String filterProgram,
            String filterVersion,
            String refcodeList) throws JAXBException {

        if (xmlObj == null) {
            throw new IllegalArgumentException(XML_OBJ_REQUIRED);
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            writeFile(xmlObj, baos, filterUser, filterSource, filterProgram, filterVersion, refcodeList);
            return new ByteArrayInputStream(baos.toByteArray());

        } catch (JAXBException e) {
            throw new IllegalStateException("Failed to marshal ListType to XML", e);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to append footer", e);
        }
    }

    public static void writeFile(
            ListType xmlObj,
            OutputStream out,
            String filterUser,
            String filterSource,
            String filterProgram,
            String filterVersion,
            String refcodeList) throws JAXBException, IOException {

        if (xmlObj == null) {
            throw new IllegalArgumentException(XML_OBJ_REQUIRED);
        }
        Marshaller marshaller = listTypeContext().createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.name());
        marshaller.marshal(xmlObj, out);
        out.write(footer(filterUser, filterSource, filterProgram, filterVersion, refcodeList).getBytes(StandardCharsets.UTF_8));
    }

    private static String footer(String filterUser, String filterSource, String filterProgram, String filterVersion,
                                 String refcodeList) {
        String now = LocalDateTime.now(ZoneOffset.UTC).format(FOOTER_TIME);

        StringBuilder footerBuilder = new StringBuilder();
        footerBuilder.append(FOOTER_LINE);
        footerBuilder.append("<!--     ").append(now).append("  -->\n");
        footerBuilder.append("<!--     Content filter by [")
                .append(nullToEmpty(filterUser)).append("] on [")
                .append(nullToEmpty(filterSource)).append("]  -->\n");
        footerBuilder.append("<!--     with [")
                .append(nullToEmpty(filterProgram)).append("] - [")
                .append(nullToEmpty(filterVersion)).append("]  -->\n");
        footerBuilder.append("<!--          Refcode : ")
                .append(nullToEmpty(refcodeList)).append("  -->\n");
        footerBuilder.append(FOOTER_LINE);
        return footerBuilder.toString();
    }

    public static void writeFile(ListType xmlObj, OutputStream out, List<SixFilters> filters) throws JAXBException, IOException {
        if (xmlObj == null) {
            throw new IllegalArgumentException(XML_OBJ_REQUIRED);
        }
        Marshaller marshaller = listTypeContext().createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.name());
        marshaller.marshal(xmlObj, out);
        out.write(footer(filters).getBytes(StandardCharsets.UTF_8));
    }

    static String footer(List<SixFilters> filters) {
        StringBuilder footer = new StringBuilder(FOOTER_LINE);
        comment(footer, LocalDateTime.now(ZoneOffset.UTC).format(FOOTER_TIME));
        comment(footer, "Content filtered by");
        if (filters == null) {
            comment(footer, "filters not available");
        } else if (filters.isEmpty()) {
            comment(footer, "no active filter");
        } else {
            filters.stream().filter(Objects::nonNull).sorted(FILTER_ORDER).forEach(filter -> {
                String prefix = "[" + nullToEmpty(filter.getFileType()) + "] " + nullToEmpty(filter.getFilterName()) + " : ";
                comment(footer, prefix + conditions(filter));
                if (filter.isExcludeCMIC()) {
                    comment(footer, prefix + "rows of the CMIC rule excluded");
                }
                if (filter.isExcludeE014071()) {
                    comment(footer, prefix + "rows of the E014071 rule excluded");
                }
            });
        }
        footer.append(FOOTER_LINE);
        return footer.toString();
    }

    private static String conditions(SixFilters filter) {
        try {
            return describe(filter);
        } catch (RuntimeException e) {
            log.warn("SIX footer: conditions of filter {} not available: {}", filter.getFilterName(), e.getMessage());
            return "conditions not available";
        }
    }

    private static String describe(SixFilters filter) {
        String conditions = filter.getActiveSixSubfilters().stream()
                .sorted(Comparator.comparing(SixSubFilters::getSixIndex, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(sub -> sub.getActiveSixSubORfilters().stream()
                        .sorted(Comparator.comparing(SixSubORFilters::getSixORIndex, Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(or -> nullToEmpty(or.getFieldName()) + " " + nullToEmpty(or.getOperator()) + " " + nullToEmpty(or.getFilterValue()))
                        .collect(Collectors.joining(" OR ", "(", ")")))
                .filter(group -> !"()".equals(group))
                .collect(Collectors.joining(" AND "));
        return conditions.isEmpty() ? "no condition" : conditions;
    }

    private static void comment(StringBuilder footer, String text) {
        String safe = text.replaceAll("[\\r\\n\\t]+", " ");
        while (safe.contains("--")) {
            safe = safe.replace("--", "- -");
        }
        footer.append("<!-- ").append(safe).append(" -->\n");
    }

    private static int fileTypeRank(String fileType) {
        int rank = FILE_TYPE_ORDER.indexOf(fileType == null ? "" : fileType.trim().toLowerCase(Locale.ROOT));
        return rank < 0 ? FILE_TYPE_ORDER.size() : rank;
    }

    private static JAXBContext listTypeContext() throws JAXBException {
        JAXBContext context = LIST_TYPE_CONTEXT.get();
        if (context == null) {
            LIST_TYPE_CONTEXT.compareAndSet(null, JAXBContext.newInstance(ListType.class));
            context = LIST_TYPE_CONTEXT.get();
        }
        return context;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}

