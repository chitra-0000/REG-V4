package com.bnpp.regliss.vo;

public class ImportLineErrorReport {
    public enum ErrorType {
        BLOCKING, NON_BLOCKING
    }

    public String externalReference;
    public String errorDescription;
    public String errorDetails;
    public String toastMessage;
    public ErrorType type;
    public String field;
    public String value;

    public ImportLineErrorReport(){}

    private ImportLineErrorReport(ImportLineErrorReportBuilder builder) {
        this.externalReference = builder.externalReference;
        this.errorDescription = builder.errorDescription;
        this.errorDetails = builder.errorDetails;
        this.toastMessage = builder.toastMessage;
        this.type = builder.type;
        this.field = builder.field;
        this.value = builder.value;
    }

    public static class ImportLineErrorReportBuilder{
        private String externalReference;
        private String errorDescription;
        private String errorDetails;
        private String toastMessage;
        private ErrorType type;
        private String field;
        private String value;

        public ImportLineErrorReportBuilder(String errorDescription) {
            this.errorDescription = errorDescription;
        }
        public ImportLineErrorReportBuilder withExternalReference(String externalReference) {
            this.externalReference = externalReference;
            return this;
        }
        public ImportLineErrorReportBuilder withErrorDetails(String errorDetails) {
            this.errorDetails = errorDetails;
            return this;
        }
        public ImportLineErrorReportBuilder withToastMessage(String toastMessage) {
            this.toastMessage = toastMessage;
            return this;
        }
        public ImportLineErrorReportBuilder withType(ErrorType type) {
            this.type = type;
            return this;
        }
        public ImportLineErrorReportBuilder withField(String field) {
            this.field = field;
            return this;
        }
        public ImportLineErrorReportBuilder withValue(String value) {
            this.value = value;
            return this;
        }
        public ImportLineErrorReport build() {
            return new ImportLineErrorReport(this);
        }
    }
}
