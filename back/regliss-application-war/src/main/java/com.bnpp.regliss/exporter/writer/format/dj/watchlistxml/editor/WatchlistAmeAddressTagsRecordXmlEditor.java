package com.bnpp.regliss.exporter.writer.format.dj.watchlistxml.editor;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

@UtilityClass
public class WatchlistAmeAddressTagsRecordXmlEditor {
    private final String ENTITY_DETAILS_ELEM_NAME = "CompanyDetails";
    private final String ADDRESS_LINE_ELEM_NAME = "AddressLine";
    private final String ADDRESS_ELEM_NAME = "Address";
    private final String ADDRESS_CITY_ELEM_NAME = "AddressCity";
    private final String DEFAULT_VALUE_FOR_MISSING_ADDRESS_CITY = "unknown";

    public void addUnknownAddressCityTags(Document doc) {
        Element root = doc.getDocumentElement();
        if (WatchlistAmeGeneralRecordXmlEditor.isPerson(root)) {
            addUnknownCityTagToPersonRecordsWithoutCity( root);
        }else if(WatchlistAmeGeneralRecordXmlEditor.isEntity(root)){
            addUnknownCityTagToEntityRecordsWithoutCity( root);
        }
    }
    private void addUnknownCityTagToEntityRecordsWithoutCity( Element root) {
        NodeList possiblyAddressTags = root.getElementsByTagName(ENTITY_DETAILS_ELEM_NAME);
        addUnknownCityToAddressTagWithoutCity(root, possiblyAddressTags);

    }
    private void addUnknownCityTagToPersonRecordsWithoutCity( Element root) {
        NodeList possiblyAddressTags = root.getElementsByTagName(ADDRESS_ELEM_NAME);
        addUnknownCityToAddressTagWithoutCity(root, possiblyAddressTags);
    }

    private void addUnknownCityToAddressTagWithoutCity(Element root, NodeList possiblyAddressTags) {
        Predicate<Element> addressCityEmpty = add -> add.getElementsByTagName(ADDRESS_CITY_ELEM_NAME).getLength() == 0;

        List<Element> addressesWithoutCity = IntStream.range(0, possiblyAddressTags.getLength())
            .mapToObj(i -> (Element) possiblyAddressTags.item(i))
            .filter(addressLineExists().and(addressCityEmpty))
            .collect(toList());
        addressesWithoutCity.forEach(address -> insertCityAfterAddressLine(root.getOwnerDocument(), address));
    }

    private void insertCityAfterAddressLine(Document doc, Element address) {
        Element addressCity = doc.createElement(ADDRESS_CITY_ELEM_NAME);
        addressCity.setTextContent(DEFAULT_VALUE_FOR_MISSING_ADDRESS_CITY);
        List<String> elementsAfterDescriptions = Arrays.asList("AddressCountry", "URL");
        WatchlistAmeGeneralRecordXmlEditor.insertElementBeforeFirstExistingElementInList(address, addressCity, elementsAfterDescriptions);
    }

    private Predicate<Element> addressLineExists() {
        Predicate<Element> addressLineTagExists = add -> add.getElementsByTagName(ADDRESS_LINE_ELEM_NAME).getLength() > 0;
        Predicate<Element> addressLineIsNotEmpty = add -> StringUtils.isNotBlank(add.getElementsByTagName(ADDRESS_LINE_ELEM_NAME)
                .item(0).getTextContent());
        return addressLineTagExists.and(addressLineIsNotEmpty);
    }
}
