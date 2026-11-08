package com.bnpp.regliss.importer.dj;

// [MISSING FROM SCREENSHOTS: lines 3-19 of the original file were not visible in the photos]

@Slf4j
@Component
@ReglissBatchProfile
public class XsltTransformerFactory {

    @Value("${automatic.import.transformer.DOW_JONES_WATCHLIST}")
    private Resource transformerFileWatchlistAme;

    @Value("${automatic.import.transformer.DOW_JONES_CORE}")
    private Resource transformerFileCore;

    @Value("${automatic.import.transformer.CUSTOM_AUTOMATIC}")
    private Resource transformerFileCustomAutomatic;
    @Autowired
    private XsltApi xsltApi;

    public Transformer getTransformerByFileType(ImportFileType fileType, DJXmlReferentials xmlReferentials, DbReferentials dbReferentials) {
        try {
            initXsltApi(xmlReferentials, dbReferentials);
            StreamSource xlstInputStream = new StreamSource(getTransformerFileByType(fileType).getInputStream());
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "");
            transformerFactory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalStylesheet", "");
            log.info("Using transformer factory {}", transformerFactory.getClass().getName());
            return transformerFactory.newTransformer(xlstInputStream); // NOSONAR: the location of the XSLT files is configured from the datab
        } catch (TransformerConfigurationException | TransformerFactoryConfigurationError | IOException e) {
            throw new ReglissException(e);
        }
    }

    public void initXsltApi(DJXmlReferentials xmlReferentials, DbReferentials dbReferentials) {
        log.debug("Setting xml referentials on thread: {}", xmlReferentials);
        log.debug("Setting db referentials on thread: {}", dbReferentials);
        xsltApi.setXmlReferentials(xmlReferentials);
        xsltApi.setDbReferentials(dbReferentials);
    }

    private Resource getTransformerFileByType(ImportFileType fileType) {
        switch (fileType) {
            case DOW_JONES_CORE:
                return transformerFileCore;
            case DOW_JONES_WATCHLIST:
            case DOW_JONES_WATCHLIST_EXTRA:
            case DOW_JONES_AME:
                return transformerFileWatchlistAme;
            case CUSTOM:
            case SIX_MAIN_FILE:
                return transformerFileCustomAutomatic;
            default:
                throw new IllegalArgumentException();
        }
    }
}

