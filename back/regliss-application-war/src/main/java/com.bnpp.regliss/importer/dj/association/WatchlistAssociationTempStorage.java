package com.bnpp.regliss.importer.dj.association;

// [MISSING FROM SCREENSHOTS: lines 3-17 of the original file were not visible in the photos]

@Slf4j
public class WatchlistAssociationTempStorage implements IAssociationTempStorage {

    private final File tempFile;
    private final Map<String, AssociationInTempFile> inFileLocations = new HashMap<>();


    public WatchlistAssociationTempStorage(File tempFile) {
        log.debug("Using temp file: {}", tempFile);
        this.tempFile = tempFile;
    }

    @Override
    public void extractToTempFile(Supplier<? extends InputStream> xmlInputStreamSupplier) {
        deleteFileIfPresent();
        try (XmlSnippetIterable iterable = new XmlSnippetIterable(xmlInputStreamSupplier.get(),
                asList("PublicFigure", "SpecialEntity"));
                RandomAccessFile raf = new RandomAccessFile(tempFile, "rw")) {
            long lastPosition = raf.getFilePointer();
            log.debug("Started reading file...");
            for (String associationXml : iterable) {
                if (lastPosition==0) {
                    log.debug("Read the 1st association");
                }

                String id = XmlSnippetExternalReferenceExtractor.extractIdFromXml(associationXml, ImportFileType.DOW_JONES_WATCHLIST);
                raf.write(associationXml.getBytes(StandardCharsets.UTF_8));
                long endPosition = raf.getFilePointer();
                int length = (int) (endPosition - lastPosition);

                inFileLocations.put(id, new AssociationInTempFile(id, lastPosition, length));
                lastPosition = endPosition;
            }
            log.info("Read {} associations", inFileLocations.size());
        } catch (IOException e) {
            throw new ReglissException(e, "Could not store associations in temp file: " + tempFile) ;
        }
    }

    Set<String> getAllExternalReferences() { return inFileLocations.keySet(); }

    private void deleteFileIfPresent() {
        if (tempFile.isFile()) {
            log.debug("Deleting temp file: {}", tempFile);

            try {
                Files.delete(tempFile.toPath());
            } catch (IOException e) {
                throw new ReglissException(e, "Could not store delete temp file: " + tempFile) ;
            }
            log.debug("DELETED");
        }
    }

    @Override
    public IAssociationReader openReader() { return new AssociationReaderImpl(); }


    public class AssociationReaderImpl implements IAssociationReader {

        private final RandomAccessFile raf;

        public AssociationReaderImpl() {
            try {
                raf = new RandomAccessFile(tempFile, "r");
            } catch (FileNotFoundException e) {
                throw new ReglissException(e, "Could not read temp file");
            }
        }

        @Override
        public String readSnippet(String externalReference) {
            try {
                AssociationInTempFile position = inFileLocations.get(externalReference);
                if (position == null) {
                    return null;
                }
                raf.seek(position.getPosition());
                byte[] buf = new byte[position.getLength()];
                raf.read(buf);
                return new String(buf, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new ReglissException(e, "Could not retrieve association from temp file");
            }
        }

        @Override
        public void close() {
            try {
                raf.close();
            } catch (IOException e) {
                throw new ReglissException(e);
            }
        }

    }
}

