// [MISSING FROM SCREENSHOTS: lines 1-6 of the original file were not visible in the photos]


package com.bnpp.regliss.six.exporter.generated;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;

import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the generated package.
 * <p>An ObjectFactory allows you to programmatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups.  Factory methods for each of these are
 * provided in this class.
 *
 */
@XmlRegistry
public class ObjectFactory {

    private static final QName _List_QNAME = new QName("", "list");
    private static final QName _RelationTypeEx_QNAME = new QName("", "ex");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: generated
     *
     */
    public ObjectFactory() {
        // to be added
    }

    /**
     * Create an instance of {@link ListType }
     *
     */
    public ListType createListType() {
        return new ListType();
    }

    /**
     * Create an instance of {@link RecordsType }
     *
     */
    public RecordsType createRecordsType() {
        return new RecordsType();
    }

    /**
     * Create an instance of {@link RecordType }
     *
     */
    public RecordType createRecordType() {
        return new RecordType();
    }

    /**
     * Create an instance of {@link GeneralInformationType }
     *
     */
    public GeneralInformationType createGeneralInformationType() {
        return new GeneralInformationType();
    }


    /**
     * Create an instance of {@link ExtraEntityRecordDetailType }
     *
     */
    public ExtraEntityRecordDetailType createExtraEntityRecordDetailType() {
        return new ExtraEntityRecordDetailType();
    }


    /**
     * Create an instance of {@link DescriptionsType }
     *
     */
    public DescriptionsType createDescriptionsType() {
        return new DescriptionsType();
    }

    /**
     * Create an instance of {@link ProgramsType }
     *
     */
    public ProgramsType createProgramsType() {
        return new ProgramsType();
    }

    /**
     * Create an instance of {@link AliasesType }
     *
     */
    public AliasesType createAliasesType() {
        return new AliasesType();
    }

    /**
     * Create an instance of {@link SanctionsType }
     *
     */
    public SanctionsType createSanctionsType() {
        return new SanctionsType();
    }

    /**
     * Create an instance of {@link SourcesType }
     *
     */
    public SourcesType createSourcesType() {
        return new SourcesType();
    }

    /**
     * Create an instance of {@link ImagesType }
     *
     */
    public ImagesType createImagesType() {
        return new ImagesType();
    }

    /**
     * Create an instance of {@link ProgramType }
     *
     */
    public ProgramType createProgramType() {
        return new ProgramType();
    }

    /**
     * Create an instance of {@link AliasType }
     *
     */
    public AliasType createAliasType() {
        return new AliasType();
    }

    /**
     * Create an instance of {@link SanctionType }
     *
     */
    public SanctionType createSanctionType() {
        return new SanctionType();
    }

    /**
     * Create an instance of {@link AddressesType }
     *
     */
    public AddressesType createAddressesType() {
        return new AddressesType();
    }

    /**
     * Create an instance of {@link IdentityDocumentsType }
     *
     */
    public IdentityDocumentsType createIdentityDocumentsType() {
        return new IdentityDocumentsType();
    }

    /**
     * Create an instance of {@link CountryDetailsType }
     *
     */
    public CountryDetailsType createCountryDetailsType() {
        return new CountryDetailsType();
    }

    /**
     * Create an instance of {@link DatesDetailsType }
     *
     */
    public DatesDetailsType createDatesDetailsType() {
        return new DatesDetailsType();
    }

    /**
     * Create an instance of {@link FunctionsType }
     *
     */
    public FunctionsType createFunctionsType() {
        return new FunctionsType();
    }

    /**
     * Create an instance of {@link CodesType }
     *
     */
    public CodesType createCodesType() {
        return new CodesType();
    }

    /**
     * Create an instance of {@link RelationsType }
     *
     */
    public RelationsType createRelationsType() {
        return new RelationsType();
    }

    /**
     * Create an instance of {@link AddressType }
     *
     */
    public AddressType createAddressType() {
        return new AddressType();
    }

    /**
     * Create an instance of {@link DocumentType }
     *
     */
    public DocumentType createDocumentType() {
        return new DocumentType();
    }

    /**
     * Create an instance of {@link CountryType }
     *
     */
    public CountryType createCountryType() {
        return new CountryType();
    }

    /**
     * Create an instance of {@link DateType }
     *
     */
    public DateType createDateType() {
        return new DateType();
    }

    /**
     * Create an instance of {@link FunctionType }
     *
     */
    public FunctionType createFunctionType() {
        return new FunctionType();
    }

    /**
     * Create an instance of {@link CodeType }
     *
     */
    public CodeType createCodeType() {
        return new CodeType();
    }

    /**
     * Create an instance of {@link RelationType }
     *
     */
    public RelationType createRelationType() {
        return new RelationType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ListType }{@code >}
     *
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link ListType }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "list")
    public JAXBElement<ListType> createList(ListType value) {
        return new JAXBElement<>(_List_QNAME, ListType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     *
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "ex", scope = RelationType.class)
    public JAXBElement<Boolean> createRelationTypeEx(Boolean value) {
        return new JAXBElement<>(_RelationTypeEx_QNAME, Boolean.class, RelationType.class, value);
    }

}

