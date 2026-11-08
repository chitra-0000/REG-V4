package com.bnpp.regliss.importer.dj.handler;

// [MISSING FROM SCREENSHOTS: lines 3-31 of the original file were not visible in the photos]

@Slf4j
@ToString
public class TransformedRecordSAXHandler extends DefaultHandler {

    private static final String CANNOT_HAVE_CHILDREN = "Cannot have children";
    private final Deque<ParsingState> stack = new LinkedList<>();

    private StringBuilder currentText = new StringBuilder();

    public TransformedRecordSAXHandler() { stack.push(new InitialState()); }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        ParsingState newState = stack.peek().startElement(qName, attributes);
        stack.push(newState);
        currentText = new StringBuilder(); // mereu
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        currentText.append(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        stack.pop().endElement(currentText.toString());
    }


    private abstract class ParsingState {
        public abstract ParsingState startElement(String childElementName, Attributes attributes);
        public void endElement(String text) { /*ignore text content*/ }
    }

    @ToString
    private class ParsingIgnoring extends ParsingState {
        private final String elementName;
        public ParsingIgnoring(String elementName) { this.elementName = elementName; }
        public ParsingState startElement(String childElementName, Attributes attributes) {
            return new ParsingIgnoring(childElementName);
        }
        @Override
        public void endElement(String text) { log.debug("endElement action for value: {}", text); }
    }

    @ToString
    private class InitialState extends ParsingState {
        private final List<Record> records = new ArrayList<>();
        @Override
        public ParsingState startElement(String childElementName, Attributes attributes) {
            if (!"root".equals(childElementName)) {
                throw new IllegalArgumentException("No <root> root");
            }
            return new RootState(records::add);
        }
        public List<Record> getRecords() { return records; }
    }
    @ToString
    private class RootState extends ParsingState {
        private final Consumer<Record> recordConsumer;

        public RootState(Consumer<Record> recordConsumer) { this.recordConsumer = recordConsumer; }

        public ParsingState startElement(String childElementName, Attributes attributes) {
            if (hasDeleteAction(attributes)) {
                return new ParsingIgnoring(childElementName);
            } else {
                RecordType recordType = RecordType.valueOf(childElementName);
                Record record = recordType.constructor.get();
                recordConsumer.accept(record);
                return new ParsingRecord(record);
            }
        }
        private boolean hasDeleteAction(Attributes attributes) {
            String importActionValue = attributes.getValue("ACTION");
            return importActionValue != null && RecordImportAction.valueOf(importActionValue) == RecordImportAction.DELETE;
        }

    }

    @ToString
    private class ParsingRecord extends ParsingState {
        private final Record record;

        public ParsingRecord(Record record) { this.record = record; }

        public ParsingState startElement(String childElementName, Attributes attributes) {
            Field recordField = ReflectionCachedUtils.getAuditedFieldByCode(record.getClass(), childElementName);
            if (recordField != null) {
                return new ParsingRecordField(record, recordField);
            }


            Field referentialCollectionField = ReflectionCachedUtils.getImportableReferentialCollections(record.getClass(), childElementName);
            if (referentialCollectionField != null) {
                Class<? extends RecordReferential> referentialClass =
                        (Class<? extends RecordReferential>) EntityUtils.getElementType(referentialCollectionField);
                Collection<Object> referentialCollection = ReflectionUtils.getFieldValue(referentialCollectionField, record);
                return new ParsingReferentialCollection(record, referentialCollection, referentialClass);
            }


            Field collectionField = ReflectionCachedUtils.getImportableChildByType(record.getClass(), childElementName);
            if (collectionField != null) {
                Class<?> childClass = ReflectionCachedUtils.getElementType(collectionField);
                Method adder = ReflectionCachedUtils.getChildAddMethod(record.getClass(), childClass);
                AbstractSimpleEntity recordChild = (AbstractSimpleEntity) ReflectionUtils.newInstance(childClass);
                ReflectionUtils.invoke(record, adder, recordChild);
                return new ParsingRecordChild(recordChild);
            }

            return new ParsingIgnoring(childElementName);
        }
    }

    @ToString
    private class ParsingReferentialCollection extends ParsingState {
        private final Record record;
        private final Collection<Object> referentialCollection;
        private final Class<? extends RecordReferential> referentialClass;
        public ParsingReferentialCollection(Record record, Collection<Object> referentialCollection, Class<? extends RecordReferential>
                referentialClass) {
            this.record = record;
            this.referentialCollection = referentialCollection;
            this.referentialClass = referentialClass;
        }
        public ParsingState startElement(String childElementName, Attributes attributes) {
            throw new IllegalStateException(CANNOT_HAVE_CHILDREN);
        }
        @Override
        public void endElement(String text) {
            if (isNotBlank(text)) {
                RecordReferential referentialReference = getRecordReferentialReference(referentialClass, text);
                referentialCollection.add(referentialReference);
            }
        }

    }

    @ToString
    private class ParsingRecordField extends ParsingState {
        private final Record record;
        private final Field field;

        public ParsingRecordField(Record record, Field field) {
            this.record = record;
            this.field = field;
        }
        public ParsingState startElement(String childElementName, Attributes attributes) {
            throw new IllegalStateException(CANNOT_HAVE_CHILDREN);
        }
        @Override
        public void endElement(String text) { setCurrentFieldValue(record, field, text); }
    }

    @ToString
    private class ParsingRecordChild extends ParsingState {
        private final Object recordChild;

        public ParsingRecordChild(Object recordChild) { this.recordChild = recordChild; }

        public ParsingState startElement(String childElementName, Attributes attributes) {
            Field childField = ReflectionCachedUtils.getAuditedFieldByCode(recordChild.getClass(), childElementName);
            if (childField != null) {
                return new ParsingRecordChildField(recordChild, childField);
            }
            throw new IllegalStateException("Unknown record child field: " + childElementName);
        }
    }

    @ToString
    private class ParsingRecordChildField extends ParsingState {
        private final Object recordChild;
        private final Field childField;

        public ParsingRecordChildField(Object recordChild, Field childField) {
            this.recordChild = recordChild;
            this.childField = childField;
        }
        public ParsingState startElement(String childElementName, Attributes attributes) {
            throw new IllegalStateException(CANNOT_HAVE_CHILDREN);
        }
        @Override
        public void endElement(String text) { setCurrentFieldValue(recordChild, childField, text); }
    }

    private void setCurrentFieldValue(Object fieldOwner, Field field, String text) {
        if (isBlank(text)) {
            return;
        }
        Object fieldValue = getResolvedValue(field, text);
        try {
            Method setter = ReflectionCachedUtils.getSetter(field);
            ReflectionUtils.invoke(fieldOwner, setter, fieldValue);
        }catch (Exception e) {
            ReflectionUtils.setFieldValue(field, fieldOwner, fieldValue);
            log.warn("Exception!! : " + e);
        }
    }

    /unchecked, rawtypes/
    private Object getResolvedValue(Field field, String stringValue) {
        if (field.getType().isAssignableFrom(LocalDate.class)) {
            return parseDate(stringValue, ErrorCode.IMPORT_RECORD_INVALID_START_DATE_FORMAT);
        } else if (field.getType().isEnum()) {
            return Enum.valueOf((Class<Enum>) field.getType(), stringValue);
        } else if (ListReferential.class.isAssignableFrom(field.getType())) {
            Class<? extends ListReferential> labelClass = (Class<? extends ListReferential>) field.getType();
            return getLabelReference(labelClass, stringValue);
        } else if (RecordReferential.class.isAssignableFrom(field.getType())) {
            Class<? extends RecordReferential> referentialClass = (Class<? extends RecordReferential>) field.getType();
            return getRecordReferentialReference(referentialClass, stringValue);
        } else if (ClassUtils.isAssignable(Boolean.class, field.getType())) {
            return Boolean.parseBoolean(stringValue);
        } else if (field.getType().isAssignableFrom(AliasReglissReference.class)){
            return new AliasReglissReference(stringValue);
        } else {
            return stringValue;
        }
    }

    private ListReferential getLabelReference(Class<? extends ListReferential> labelClass, String idStr) {
        try {
            ListReferential label= labelClass.newInstance();
            label.setId(Long.valueOf(idStr));
            label.setLabel(idStr); // will be relinked to Hibernate session later on anyway
            return label;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ReglissException(e);
        }
    }

    private RecordReferential getRecordReferentialReference(Class<? extends RecordReferential> referentialClass, String idStr) {
        try {
            RecordReferential recordReferential= referentialClass.newInstance();
            recordReferential.setId(Long.valueOf(idStr));
            recordReferential.setLabel(idStr); // will be relinked to Hibernate session later on anyway
            return recordReferential;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ReglissException(e);
        }
    }

    private LocalDate parseDate(String s, ErrorCode errorCode) {
        String pattern = "yyyy-MM-dd";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        try {
            LocalDate date = LocalDate.parse(s, formatter);
            if (!formatter.format(date).equals(s)) {
                throw new ReglissException(errorCode, pattern);
            }
            return date;
        } catch (DateTimeParseException e) {
            throw new ReglissException(errorCode, pattern);
        }
    }


    public Optional<Record> parseRecord() {
        if (stack.peek() instanceof InitialState) {
            List<Record> records = ((InitialState)stack.peek()).getRecords();
            return records.stream().findFirst();
        } else {
            return Optional.empty();
        }
    }
}

