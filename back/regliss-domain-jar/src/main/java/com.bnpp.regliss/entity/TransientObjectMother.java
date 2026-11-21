package com.bnpp.regliss.entity;

import com.bnpp.regliss.annotation.*;
import com.bnpp.regliss.entity.User.*;
import com.bnpp.regliss.entity.builder.*;

public class TransientObjectMother {
    private TransientObjectMother() { throw new UnsupportedOperationException("Only static methods class"); }

    public static final User user1 = new UserBuilder().withId$(2L).build();
    public static final User user2 = new UserBuilder().withId$(3L).build();
    public static final User user3 = new UserBuilder().withId$(4L).build();
    public static final User user4 = new UserBuilder().withId$(5L).build();

    public static UserBuilder aTestUser() {
        return new UserBuilder().withUsername("test").withFirstName("TestF").withLastName("LastF")
                .withLanguage(Language.EN).withStatus(Status.ACTIVE).withProfile(Profile.ADMIN_MOA);
    }

    public static ReglissListBuilder aList() { return new ReglissListBuilder(); }

    public static ListCategoryBuilder aListCategory(Long id) { return new ListCategoryBuilder().withId$(id); }

    public static ListCategoryBuilder aListCategory() { return new ListCategoryBuilder(); }

    public static CountryBuilder aCountry(Long id) { return new CountryBuilder().withId$(id); }

    public static CountryBuilder aCountry() { return new CountryBuilder(); }

    public static IssuerBuilder anIssuer(Long id) { return new IssuerBuilder().withId$(id); }

    public static IssuerBuilder anIssuer() { return new IssuerBuilder(); }

    public static RegulatorOrInternalPolicyBuilder anRegulatorOrInternalPolicy(Long id) {
        return new RegulatorOrInternalPolicyBuilder().withId$(id);
    }

    public static RegulatorOrInternalPolicyBuilder anRegulatorOrInternalPolicy() {
        return new RegulatorOrInternalPolicyBuilder();
    }

    public static BroadcastEmailBuilder aBroadcastEmail(Long id) { return new BroadcastEmailBuilder().withId$(id); }

    public static VersionBuilder aVersion() { return new VersionBuilder(); }

    public static VersionBuilder aVersion(Long id) { return new VersionBuilder().withId$(id); }

    public static ProgramBuilder aProgram(Long id) { return new ProgramBuilder().withId$(id); }

    public static ProgramBuilder aProgram() { return new ProgramBuilder(); }

    public static SanctionTypeBuilder aSanctionType() { return new SanctionTypeBuilder(); }

    public static AliasBuilder anAlias(Long id) { return new AliasBuilder().withId$(id); }

    public static AliasBuilder anAlias() { return new AliasBuilder(); }

    public static AuditBuilder anAudit(AuditedAction auditedAction) {
        return new AuditBuilder().withAuditedAction(auditedAction);
    }

    public static PersonRecordBuilder aPersonRecord() { return new PersonRecordBuilder(); }

    public static AddressBuilder anAddress() { return new AddressBuilder(); }
}
