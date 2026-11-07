package com.bnpp.regliss.exporter.launcher;

import com.bnpp.regliss.exporter.writer.format.dj.ExportDJXmlUtils;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.collections.ListUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

@EqualsAndHashCode
public class WatchlistAmeOriginalCategoryIds {

    private static final String DESCRIPTION1_LIST_ELEM_NAME = "Description1List";
    private static final String DESCRIPTION1_ID_ATTRIBUTE_NAME = "Description1Id";

    @Getter(AccessLevel.PACKAGE)
    private final List<String> pepCategoryIds;
    @Getter(AccessLevel.PACKAGE)
    private final List<String> rcaCategoryIds;

    public WatchlistAmeOriginalCategoryIds(String referentialSnippet, List<String> pepCategoryLabels, List<String> rcaCategoryLabels) {
        pepCategoryIds = getCategoryIdsByLabelsInDescription1List(referentialSnippet, pepCategoryLabels);
        rcaCategoryIds = getCategoryIdsByLabelsInDescription1List(referentialSnippet, rcaCategoryLabels);
    }

    public WatchlistAmeOriginalCategoryIds( List<String> pepCategoryIds, List<String> rcaCategoryIds) {
        this.pepCategoryIds = pepCategoryIds;
        this.rcaCategoryIds = rcaCategoryIds;
    }

    private List<String> getCategoryIdsByLabelsInDescription1List(String prefixReferentialSnippet, List<String> categoryLabels) {
        NodeList descriptions1Names = ExportDJXmlUtils.getDocumentWithFictionalRoot(prefixReferentialSnippet)
                .getElementsByTagName(DESCRIPTION1_LIST_ELEM_NAME)
                .item(0)
                .getChildNodes();
        return IntStream.range(0, descriptions1Names.getLength())
                .mapToObj(descriptions1Names::item)
                .filter(description1NameElement -> categoryLabels.contains(description1NameElement.getTextContent()))
                .map(description1NameElement -> ((Element) description1NameElement).getAttribute(DESCRIPTION1_ID_ATTRIBUTE_NAME))
                .collect(toList());
    }

    public List<String> getPepOrRcaCategoryIds() { return ListUtils.union(pepCategoryIds, rcaCategoryIds); }
}
