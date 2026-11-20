package com.bnpp.regliss.importer;

// [MISSING FROM SCREENSHOTS: lines 3-5 of the original file were not visible in the photos]

@Data
class DJProgram {

    private final String label;
    private final String description;
    private final Long sanctionTypeId;


    public DJProgram(Program program) {
        this.label = program.getLabelOpt().orElse(null);
        this.description = program.getDescription();
        this.sanctionTypeId = program.getSanctionTypeOpt()
                .map(ProgramSanctionType::getId).orElse(null);
    }

    @Override
    public String toString() {
        return "DJProgram{" +
                "label='" + label + '\'' +
                ", description='" + description + '\'' +
                ", sanctionTypeId=" + sanctionTypeId +
                '}';
    }
}
