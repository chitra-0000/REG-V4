package com.bnpp.regliss.importer.crb;

// [MISSING FROM SCREENSHOTS: lines 3-11 of the original file were not visible in the photos]

@Getter
@Setter
public class CRBRecordReferential {
    private final String descTierCategory = "CRB Tier category";
    private final String descPercentDirectCannabisRevenue = "% Direct Cannabis Revenue";

    private String securitiesId = null;
    private String crbmId = null;
    private String primaryBusinessName = null;
    private String tier = null;
    private String securityType = null;
    private String securityStatus = null;
    private String cusip = null;
    private String isin = null;
    private String sedol = null;
    private String primarySymbol = null;
    private String primaryExchange = null;
    private String secondSymbol = null;
    private String secondExchange = null;
    private String thirdSymbol = null;
    private String thirdExchange = null;
    private String fourthSymbol = null;
    private String fourthExchange = null;
    private String licensedCountries = null;
    private String licenseClassifications = null;
    private String licenseTypes = null;
    private String licenseStatuses = null;
    private String sectors = null;
    private String dividend = null;
    private String purePlay = null;
    private String percentDirectCannabisRevenue = null;

    public Set<CodeCusip> getCodeCusip() {
        if (this.cusip == null) {
            return null;
        }
        CodeCusip code = new CodeCusip();
        code.setCode(this.cusip);
        Set<CodeCusip> codeCusip = new HashSet<>();
        codeCusip.add(code);
        return codeCusip;
    }

    public Set<CodeIsin> getCodeIsin() {
        if (this.isin == null) {
            return null;
        }
        CodeIsin code = new CodeIsin();
        code.setCode(this.isin);
        Set<CodeIsin> codeIsin = new HashSet<>();
        codeIsin.add(code);
        return codeIsin;
    }

    public Set<IdentityDocument> getIdentityDocument() {
        IdentityDocument identity = new IdentityDocument();
        IdentityDocumentType identityType = new IdentityDocumentType();
        identity.setType(identityType.setId(7L));
        identity.setNumber(this.percentDirectCannabisRevenue);
        identity.setAdditionalInfo(descPercentDirectCannabisRevenue);
        Set<IdentityDocument> identityDocument = new HashSet<>();
        identityDocument.add(identity);
        return identityDocument;
    }

    public Set<Program> getSanctionProgram() {
        Program program = new Program();
        ProgramSanctionType programType = new ProgramSanctionType();
        program.setSanctionType(programType.setId(1181));
        program.setLabel("Tier " + this.tier);
        program.setDescription(descTierCategory);
        Set<Program> programSet = new HashSet<>();
        programSet.add(program);
        return programSet;
    }

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append("securitiesId = " + securitiesId + "\n")
                .append("crbmId = " + crbmId + "\n")
                .append("primaryBusinessName = " + primaryBusinessName + "\n")
                .append("tier = " + tier + "\n")
                .append("securityType = " + securityType + "\n")
                .append("securityStatus = " + securityStatus + "\n")
                .append("cusip = " + cusip + "\n")
                .append("isin = " + isin + "\n")
                .append("sedol = " + sedol + "\n")
                .append("primarySymbol = " + primarySymbol + "\n")
                .append("primaryExchange = " + primaryExchange + "\n")
                .append("secondSymbol = " + secondSymbol + "\n")
                .append("secondExchange = " + secondExchange + "\n")
                .append("thirdSymbol = " + thirdSymbol + "\n")
                .append("thirdExchange = " + thirdExchange + "\n")
                .append("fourthSymbol = " + fourthSymbol + "\n")
                .append("fourthExchange = " + fourthExchange + "\n")
                .append("licensedCountries = " + licensedCountries + "\n")
                .append("licenseClassifications = " + licenseClassifications + "\n")
                .append("licenseTypes = " + licenseTypes + "\n")
                .append("licenseStatuses = " + licenseStatuses + "\n")
                .append("Sectors = " + sectors + "\n")
                .append("dividend = " + dividend + "\n")
                .append("purePlay = " + purePlay + "\n")
                .append("percentDirectCannabisRevenue = " + percentDirectCannabisRevenue + "\n");
        return result.toString();
    }
}

