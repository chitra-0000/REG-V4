package com.bnpp.regliss.entity;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.annotation.EqualityField;
import com.bnpp.regliss.entity.referential.Scope;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.*;

import static com.bnpp.regliss.ReglissException.ErrorCode.GENERAL;

@Entity
@Table(name = "EMAIL_TEMPLATE")
/**@SequenceGenerator(name = "EmailTemplateIdGenerator", initialValue = 100, sequenceName = "EMAIL_TEMPLATE_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "EmailTemplateIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "EMAIL_TEMPLATE_SEQ") } )
public class EmailTemplate extends AbstractSimpleEntity {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "EmailTemplateIdGenerator")
    protected Long id;

    @Getter @Setter
    @EqualityField
    @Column(name = "EMAIL_TYPE", unique = true, updatable = false, nullable = false)
    @Enumerated(EnumType.STRING)
    private EmailType type;

    @Getter @Setter
    @EqualityField
    @Column(name = "TO_ADRESS")
    private String toAddress;

    @Getter @Setter
    @EqualityField
    @Column(name = "GROUP_TO_ADRESS")
    private String groupToAddress;

    @Getter @Setter
    @EqualityField
    @Column(name = "CC_ADDRESS")
    private String ccAddress;

    @Getter @Setter
    @EqualityField
    @Column(name = "GROUP_CC_ADRESS")
    private String groupCcAddress;

    @Getter @Setter
    @EqualityField
    @Column(name = "SUBJECT_TEMPLATE_FR")
    private String subjectTemplateFR;

    @Getter @Setter
    @EqualityField
    @Column(name = "SUBJECT_TEMPLATE_EN")
    private String subjectTemplateEN;

    @Getter @Setter
    @EqualityField
    @Column(name = "BODY_TEMPLATE_FR", length=1000) // length is required by hsqldb
    @Lob
    private String bodyTemplateFR;

    @Getter @Setter
    @EqualityField
    @Column(name = "BODY_TEMPLATE_EN", length=1000) // length is required by hsqldb
    @Lob
    private String bodyTemplateEN;

    public String[] getToAddresses() { return splitAddresses(toAddress); }
    public String[] getCcAddresses() { return splitAddresses(ccAddress); }

    public String[] getGroupToAddresses() { return splitAddresses(groupToAddress); }

    public String[] getAllToAddresses() { return ArrayUtils.addAll(getToAddresses(),getGroupToAddresses()); }

    public String[] getAllCcAddresses() { return ArrayUtils.addAll(getCcAddresses(),getGroupCcAddresses()); }

    public String[] getGroupCcAddresses() { return splitAddresses(groupCcAddress); }

    private String[] splitAddresses(String adressList){
        if(adressList == null){
            return null; // NOSONAR do not touch
        }
        return adressList.split(",");
    }

    public String[] getToAddresses(Scope scope){
        switch(scope){
            case LOCAL:
                return getToAddresses();
            case GROUP:
                return getGroupToAddresses();
            default:
                throw new ReglissException(GENERAL,"Cannot get email address for list in this scope");
        }

    }

    public String[] getCcAddresses(Scope scope){
        switch(scope){
            case LOCAL:
                return getCcAddresses();
            case GROUP:
                return getGroupCcAddresses();
            default:
                throw new ReglissException(GENERAL,"Cannot get email address for list in this scope");
        }
    }
}
