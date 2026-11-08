package com.bnpp.regliss.importer.dj.handler;

// [MISSING FROM SCREENSHOTS: lines 3-24 of the original file were not visible in the photos]

@Slf4j
@Service
@ReglissBatchProfile
public class WatchlistAssociationParser {

    @Autowired
    private RelationTypeRepository relationTypeRepo;

    public interface AssociationContainer {
        List<AssociateXml> getAssociates();
    }

    @Data
    @XmlRootElement(name="PublicFigure")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class PublicFigureXml implements AssociationContainer {
        @XmlAttribute
        private String id;
        @XmlElement(name = "Associate")
        private List<AssociateXml> associates;
    }

    @Data
    @XmlRootElement(name="SpecialEntity")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class SpecialEntityXml implements AssociationContainer {
        @XmlAttribute
        private String id;
        @XmlElement(name = "Associate")
        private List<AssociateXml> associates;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class AssociateXml {
        @XmlAttribute
        private String id;
        @XmlAttribute
        private String code;
        @XmlAttribute
        private String ex;
    }

    private static JAXBContext jaxbContext;
    static {
        try {
            jaxbContext = JAXBContext.newInstance(PublicFigureXml.class, AssociateXml.class, SpecialEntityXml.class);
        } catch (JAXBException e) {
            throw new ReglissException(e);
        }
    }
    public Set<RecordAssociation> parse(String associationsXml, Set<DJXmlReferential> xmlRelationTypes) {
        try {
            Unmarshaller um = jaxbContext.createUnmarshaller();
            AssociationContainer associationContainer = (AssociationContainer) um.unmarshal(new StringReader(associationsXml));
            return associationContainer.getAssociates().stream()
                    .map(xml -> toAssociation(xml, xmlRelationTypes))
                    .collect(toSet());
        } catch (JAXBException e) {
            throw new ReglissException(e);
        }
    }

    private RecordAssociation toAssociation(AssociateXml xml, Set<DJXmlReferential> xmlRelationTypes) {
        RecordAssociation association = new RecordAssociation();
        association.setExternalReference(xml.getId());
        association.setEx("Yes".equalsIgnoreCase(xml.getEx()));
        association.setRelationType(matchRelationType(xml.getCode(), xmlRelationTypes));
        return association;

    }

    private RelationType matchRelationType(String code, Set<DJXmlReferential> xmlRelationTypes) {
        Optional<DJXmlReferential> labelInFile = xmlRelationTypes.stream()
                .filter(djRef -> StringUtils.equals(djRef.getCode(), code))
                .findFirst();
        if (labelInFile.isPresent()) {
            return relationTypeRepo.findAll().stream()
                    .filter(rt -> StringUtils.equals(labelInFile.get().getValue(), rt.getLabel()))
                    .findFirst()
                    .orElse(null);
        } else {
            log.error("No relation type found for code: " + code);
            return null;
        }
    }

}

