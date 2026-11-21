package com.bnpp.regliss.entity.record;

import com.bnpp.regliss.annotation.EqualityField;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

@Embeddable
public class AliasReglissReference implements ToStringAuditable {

    @EqualityField
    @Column(name = "RECORD_REGLISS_REFERENCE")
    private String recordReglissReference;

    @Getter
    @EqualityField
    @Column(name = "REGLISS_REFERENCE_SUFFIX")
    private String suffix;

    public AliasReglissReference() {}

    public AliasReglissReference(String fullReglissReference) {
        Pattern p = Pattern.compile("([0-9]{1,}+).([0-9]{1,}+).([0-9]{3,}+)([A-Z]{1,})");
        Matcher m = p.matcher(fullReglissReference);
        if(!m.matches()){
            throw new IllegalArgumentException("Alias's regliss reference '" + fullReglissReference + "' does not match expected pattern: " + p.pattern());
        }
        this.recordReglissReference = m.group(1)+ "." + m.group(2) + "." + m.group(3);
        this.suffix = m.group(4);
    }

    public AliasReglissReference(String recordReglissReference, String reglissReferenceSuffix) {
        this.recordReglissReference = recordReglissReference;
        this.suffix = reglissReferenceSuffix;
    }

    public AliasReglissReference(AliasReglissReference other) {
        this.recordReglissReference = other.recordReglissReference;
        this.suffix = other.suffix;
    }

    public String getFullReference() {
        return Stream.of(recordReglissReference, suffix)
                .filter(StringUtils::isNotEmpty)
                .collect(joining());
    }

    @Override
    public String toStringAudit() { return getFullReference(); }
}
