package com.bnpp.regliss.importer.dj;
// [MISSING FROM SCREENSHOTS: lines 2-97 of the original file were not visible in the photos]
                    log.warn(LINE+ "{} : {}" ,exception.getLineNumber() , exception.getMessage());
                }
                public void fatalError(SAXParseException exception) throws SAXException {
                    errors.add(LINE+exception.getLineNumber() + " : " + exception.getMessage());
                }
                public void error(SAXParseException exception) throws SAXException {
                    errors.add(LINE+exception.getLineNumber() + " : " + exception.getMessage());
                }
            });
            validator.validate(new StreamSource(xmlStream));
        } catch (IOException | SAXException e) {
            log.info("Exception: {}", e.getMessage());
            return singletonList(e.getMessage());
        }
        return errors;
    }

    private File getSchemaFile(ImportFileType fileType) {
        switch (fileType) {
            case DOW_JONES_CORE:
                return coreXsd;
            case DOW_JONES_WATCHLIST:
            case DOW_JONES_AME:
            case DOW_JONES_WATCHLIST_EXTRA:
                return watchlistAmeXsd;
            case CUSTOM:
            case SIX_MAIN_FILE:
                return customAutomaticXsd;
            case SIX_INSTRUMENTS_FILE:
                return sixInstrumentAutomaticXsd;
            case SIX_STRUCTURED_FILE:
                return sixStructureAutomaticXsd;
            case SIX_OPTIONS_FILE:
                return sixOptionsAutomaticXsd;
            default:
                throw new IllegalArgumentException();
        }
    }

}

