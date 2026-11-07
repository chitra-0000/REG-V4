package com.bnpp.regliss.exporter.writer.format.dj.watchlistxml.editor;

import lombok.experimental.UtilityClass;
import org.springframework.data.util.Optionals;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import java.util.List;
import java.util.Optional;

@UtilityClass
class WatchlistAmeGeneralRecordXmlEditor {
    public final String DESCRIPTIONS_ELEM_NAME = "Descriptions";
    public final String DESCRIPTION_ELEM_NAME = "Description";
    public final String DESCRIPTION1_ELEM_NAME = "Description1";
    boolean isPerson(Element root) { return root.getTagName().equalsIgnoreCase("person"); }
    boolean isEntity(Element root) { return root.getTagName().equalsIgnoreCase("entity"); }
    void insertElementBeforeFirstExistingElementInList(Element parentElement, Element childElement, List<String> elementsToTryToInsertBefore) {
        Optional<Node> elementToAppendBeforeIt = elementsToTryToInsertBefore.stream()
            .filter(e -> parentElement.getElementsByTagName(e).getLength() > 0)
            .map(e -> parentElement.getElementsByTagName(e).item(0))
            .findFirst();
        Optionals.ifPresentOrElse(
            elementToAppendBeforeIt,
            e -> parentElement.insertBefore(childElement, e),
            () -> parentElement.appendChild(childElement)
        );
    }
}
