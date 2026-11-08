package com.bnpp.regliss.importer.dj.referential.xml;

// [MISSING FROM SCREENSHOTS: lines 3-7 of the original file were not visible in the photos]

@Data
@EqualsAndHashCode(callSuper = true)
public class DJXmlReferentialWithParentCode extends DJXmlReferential {

    private String parentCode;

    public DJXmlReferentialWithParentCode(String code, String value, String parentCode) {
        super(code, value);
        this.parentCode = parentCode;
    }

    public DJXmlReferentialWithParentCode() {
    }

    public static Predicate<DJXmlReferential> byCodeAndParent(String code, String parentCode) {
        return ref -> {
            DJXmlReferentialWithParentCode other = (DJXmlReferentialWithParentCode) ref;
            return Objects.equals(other.getCode(), code) &&
                    Objects.equals(other.parentCode, parentCode);
        };
    }
}

