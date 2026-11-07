package com.bnpp.regliss.exporter.launcher.filtering.watchlistame;

import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.exporter.launcher.RecordFilterInclusionTag;
import com.bnpp.regliss.exporter.writer.format.dj.ExportDJXmlUtils;
import com.bnpp.regliss.vo.export.dj.watchlist.FilterIdTag;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class WatchlistAmeFilterTagsToDescription2Details {
    private Map<RecordFilterInclusionTag, Integer> filterTagsToDescription2Ids;
    private String enrichedReferentialXmlPrefix;
    private int minDescription1Id;
    //HEAVY BUSINESS LOGIC CONSTRUCTOR, BE CAREFUL WHEN CHANGING THE LOGIC OR THE ORDER OF THE METHODS (DDD style, why
    // have anemic data structures?)
    public WatchlistAmeFilterTagsToDescription2Details(List<FilterIdTag> filterIdTags, Version version) {
        Document referentialDocument = ExportDJXmlUtils.getDocumentWithFictionalRoot(version.getVersionDJ().getReferentialsXmlPrefixSnippet());
        filterTagsToDescription2Ids = generateFilterTagsToDescription2IdsMap(filterIdTags);
        minDescription1Id = getMinDescription1Id(referentialDocument.getDocumentElement());
        enrichedReferentialXmlPrefix = enrichReferentialXmlPrefix(referentialDocument, getFilterTagsToDescription2Ids(), minDescription1Id);
    }

    private Map<RecordFilterInclusionTag, Integer> generateFilterTagsToDescription2IdsMap(List<FilterIdTag> filterTags) {
        Map<RecordFilterInclusionTag, Integer> filterTagsToDescription2IdsMap = new HashMap<>();
        AtomicInteger idSupplier = new AtomicInteger(100); //Start from 100 to avoid collisions with Watchlist
        // Description2 referential ids (decided by business)
        filterTags.forEach(f -> {
            filterTagsToDescription2IdsMap.put(RecordFilterInclusionTag.includedFilterTag(f.getFilterTag()), idSupplier.getAndIncrement());
            filterTagsToDescription2IdsMap.put(RecordFilterInclusionTag.excludedFilterTag(f.getFilterTag()), idSupplier.getAndIncrement());
        });
        filterTagsToDescription2IdsMap.put(RecordFilterInclusionTag.OTHER, idSupplier.getAndIncrement());
        return filterTagsToDescription2IdsMap;
    }

    private int getMinDescription1Id(Element root) {
        NodeList description2Names = root.getElementsByTagName("Description2Name");
        return IntStream.range(0, description2Names.getLength()).mapToObj(description2Names::item)
                .map(n -> ((Element) n).getAttribute("Description1Id"))
                .filter(StringUtils::isNotBlank)
                .mapToInt(Integer::parseInt)
                .min()
                .orElseThrow(() -> new IllegalStateException("Watchlist/Ame referential list should have at least 1 Description1 id"));
    }

    private Element createDescription2NameTagElement(Document referentialDocument, int minDescription1Id, int includedTagId,
                                                     RecordFilterInclusionTag recordFilterInclusionTag) {
        Element inclusionTag = referentialDocument.createElement("Description2Name");
        inclusionTag.setAttribute("Description1Id", Integer.toString(minDescription1Id));
        inclusionTag.setAttribute("Description2Id", Integer.toString(includedTagId));
        inclusionTag.setTextContent(recordFilterInclusionTag.getExportValue());
        return inclusionTag;
    }

    private String enrichReferentialXmlPrefix(Document referentialDocument, Map<RecordFilterInclusionTag,
            Integer> filterTagsToDescription2Ids, int minDescription1Id) {
        Node description2List = referentialDocument.getElementsByTagName("Description2List").item(0);

        filterTagsToDescription2Ids.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEach(e -> {

            Element tagElement = createDescription2NameTagElement(referentialDocument, minDescription1Id, e.getValue(), e.getKey());

            if (description2List == null) {
                throw new IllegalStateException("Should have Description2List");
            }

            description2List.appendChild(tagElement);
        });

        return ExportDJXmlUtils.removeFictionalRoot(referentialDocument);
    }
}
