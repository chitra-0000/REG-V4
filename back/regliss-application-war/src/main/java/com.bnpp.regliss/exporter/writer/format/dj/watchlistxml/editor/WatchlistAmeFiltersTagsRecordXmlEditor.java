package com.bnpp.regliss.exporter.writer.format.dj.watchlistxml.editor;

import com.bnpp.regliss.exporter.writer.format.dj.ExportXmlTagType;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.util.Optionals;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.*;
import java.util.stream.IntStream;

import static com.bnpp.regliss.exporter.writer.format.dj.watchlistxml.editor.WatchlistAmeGeneralRecordXmlEditor.*;

@UtilityClass
public class WatchlistAmeFiltersTagsRecordXmlEditor {

    private final String DESCRIPTION2_ELEM_NAME = "Description2";

    public void addFiltersTags(Map<ExportXmlTagType, List<String>> recordTags, Document doc) {
        Element root = doc.getDocumentElement();
        Element descriptionsElement = createOrGetDescriptionsElement(doc, root);
        Optional<Element> lastDescription2AttributeElement = getLastDescription2AttributeElement(descriptionsElement);
// [MISSING FROM SCREENSHOTS: lines 25-25 of the original file were not visible in the photos]
        List<Element> filtersTagsToDescriptionElems = createFiltersTagsToDescription2Elems(recordTags, doc, lastDescription2AttributeElement);

        insertFilterTagsToDescription2IdsInTheCorrectPosition(descriptionsElement, lastDescription2AttributeElement,
                filtersTagsToDescriptionElems);
    }

    private void insertFilterTagsToDescription2IdsInTheCorrectPosition(Element descriptionsElement,
        Optional<Element> lastDescription2AttributeElement, List<Element> filtersTagsToDescriptionElems) {
        Optionals.ifPresentOrElse(
            lastDescription2AttributeElement,
            d2 -> filtersTagsToDescriptionElems.forEach(e -> descriptionsElement.insertBefore(e, d2)),
            () -> filtersTagsToDescriptionElems.forEach(descriptionsElement::appendChild)
        );
    }

    private List<Element> createFiltersTagsToDescription2Elems(Map<ExportXmlTagType, List<String>> recordTags, Document doc,
                                                               Optional<Element> lastDescription2AttributeElement) {
        String description1IdCorrespondingToInclusionFilters = recordTags.get(ExportXmlTagType.WL_DESCRIPTION1_MIN_ID).get(0);
        List<Element> filtersTagsToDescription2Elems = new ArrayList<>();

        recordTags.get(ExportXmlTagType.FILTER_INCLUSION).forEach(t -> {
            Element newDescription = createFilterTagDescription(doc, description1IdCorrespondingToInclusionFilters, t);
            filtersTagsToDescription2Elems.add(newDescription);
        });

        if (!lastDescription2AttributeElement.isPresent()) {
            String otherDescription2Id = recordTags.get(ExportXmlTagType.WL_DESCRIPTION2_OTHER).get(0); //OTHER has a business meaning here,
            // it is added only if there are no descriptions with Description2Attribute
            Element complementaryOtherDescription = createFilterTagDescription(doc, description1IdCorrespondingToInclusionFilters,
                    otherDescription2Id);
            filtersTagsToDescription2Elems.add(complementaryOtherDescription);
        }
        return filtersTagsToDescription2Elems;
    }

    private Optional<Element> getLastDescription2AttributeElement(Element descriptionsElement) {
        NodeList descriptionsNodes = descriptionsElement.getElementsByTagName(DESCRIPTION_ELEM_NAME);
        return IntStream.range(0, descriptionsNodes.getLength()).mapToObj(descriptionsNodes::item)
            .map(n -> (Element) n)
            .filter(n -> StringUtils.isNotBlank(n.getAttribute(DESCRIPTION2_ELEM_NAME)))
            .reduce((first, second) -> second)
            ;

    }

    private Element createOrGetDescriptionsElement(Document doc, Element root) {
        NodeList descriptions = doc.getElementsByTagName(DESCRIPTIONS_ELEM_NAME);
        boolean noDescriptionsElement = descriptions.getLength() == 0;

        Element descriptionsElement;
        if (noDescriptionsElement) {
            descriptionsElement = doc.createElement(DESCRIPTIONS_ELEM_NAME);
            insertDescriptionsElementInTheCorrespondingPosition(root, descriptionsElement);
        } else {
            descriptionsElement = (Element) descriptions.item(0);
        }
        return descriptionsElement;
    }

    private void insertDescriptionsElementInTheCorrespondingPosition(Element root, Element descriptionsElement) {
        List<String> elementsAfterDescriptions;

        if (WatchlistAmeGeneralRecordXmlEditor.isPerson(root)) {
            elementsAfterDescriptions = Arrays.asList("RoleDetail", "DateDetails", "BirthPlace", "SanctionsReferences", "Address",
                    "CountryDetails", "IDNumberTypes", "ProfileNotes", "SourceDescription", "Images");
        } else if (WatchlistAmeGeneralRecordXmlEditor.isEntity(root)) {
            elementsAfterDescriptions = Arrays.asList("DateDetails", "SanctionsReferences", "CompanyDetails", "VesselDetails",
                    "CountryDetails", "IDNumberTypes", "ProfileNotes", "SourceDescription");
        } else {
            throw new IllegalStateException("Records in watchlist/ame should be only of type person or entity but was " + root.getTagName());
        }

        WatchlistAmeGeneralRecordXmlEditor.insertElementBeforeFirstExistingElementInList(root, descriptionsElement, elementsAfterDescriptions);
    }

    private Element createFilterTagDescription(Document doc, String description1IdCorrespondingToInclusionFilters, String t) {
        Element newDescription = doc.createElement(DESCRIPTION_ELEM_NAME);
        newDescription.setAttribute(DESCRIPTION1_ELEM_NAME, description1IdCorrespondingToInclusionFilters);
        newDescription.setAttribute(DESCRIPTION2_ELEM_NAME, t);
        return newDescription;
    }
}
