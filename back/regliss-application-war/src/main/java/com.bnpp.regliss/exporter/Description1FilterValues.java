package com.bnpp.regliss.exporter;

import com.bnpp.regliss.ReglissException;

import java.util.Arrays;
import java.util.Optional;

public enum Description1FilterValues {

    PEP("Politically Exposed Person (PEP)", false, false),
    SIP("Special Interest Person (SIP)", false, false),
    SIE("Special Interest Entity (SIE)", false, false),
    RCA("Relative or Close Associate (RCA)", false, false),
    RCA_RCAA("Relative or Close Associate - All (RCAA)", true, true),
    PEP_RCA_RCAA("Politically Exposed Person (PEP)", true, true),
    SIP_RCA_RCAA("Special Interest Person (SIP)", true, true),
    SIE_RCA_RCAA("Special Interest Entity (SIE)", true, true),
    ALL("", true, true);

    private final String description1LabelEn;
    private final boolean includeRca;
    private final boolean includeRcaa;

    Description1FilterValues(String description1LabelEn, boolean includeRca, boolean includeRcaa) {
        this.description1LabelEn = description1LabelEn;
        this.includeRca = includeRca;
        this.includeRcaa = includeRcaa;
    }

    public static Description1FilterValues fromLabelEn(String v) { return getFomLabelEn(v); }

    public static Description1FilterValues fromLabelEnAndIncludeRca(String v, boolean includeRca, boolean includeRcaa) {
        return getFomLabelEnAndIncludeRcaAndRcaa(v, includeRca, includeRcaa);
    }

    private static Description1FilterValues getFomLabelEn(String labelEn) {
        Optional<Description1FilterValues> standard = Arrays.stream(Description1FilterValues.values())
                .filter(e -> e.description1LabelEn.equalsIgnoreCase(labelEn) && !e.includeRca)
                .findFirst();

        if (standard.isPresent()) {
            return standard.get();
        }
        Optional<Description1FilterValues> anyMatch = Arrays.stream(Description1FilterValues.values())
                .filter(e -> e.description1LabelEn.equalsIgnoreCase(labelEn))
                .findFirst();

        return anyMatch.orElseThrow(() -> new ReglissException("No enum value found for label: " + labelEn));
    }

    private static Description1FilterValues getFomLabelEnAndIncludeRcaAndRcaa(String labelEn, boolean includeRca, boolean includeRcaa) {
        Optional<Description1FilterValues> desc1 = Arrays.stream(Description1FilterValues.values())
                .filter(e -> e.description1LabelEn.equals(labelEn) && e.includeRca == includeRca && e.includeRcaa == includeRcaa)
                .findFirst();
        return desc1.orElseThrow(() -> new ReglissException("No enum value for label: " + labelEn));
    }

    public String toDescription1LabelEn() { return this.description1LabelEn; }

    public boolean includeRca() { return this.includeRca; }

    public boolean includeRcaa() { return this.includeRcaa; }

}
