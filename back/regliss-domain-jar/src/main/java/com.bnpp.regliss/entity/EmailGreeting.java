package com.bnpp.regliss.entity;

import com.bnpp.regliss.annotation.EqualityField;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.persistence.Entity;

@Entity
@Table(name = "EMAIL_GREETING")
/**@SequenceGenerator(name = "EmailGreetingIdGenerator", initialValue = 100, sequenceName = "EMAIL_GREETING_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "EmailGreetingIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "EMAIL_GREETING_SEQ")} )
public class EmailGreeting  extends AbstractSimpleEntity{

    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "EmailGreetingIdGenerator")
    protected Long id;

    @Getter @Setter
    @EqualityField
    @Column(name = "BEGIN_GREETING")
    private String beginGreeting;

    @Getter @Setter
    @EqualityField
    @Column(name = "END_GREETING")
    private String endGreeting;

    @Getter
    @EqualityField
    @Column(name = "LANGUAGE", unique = true)
    @Enumerated(EnumType.STRING)
    private EmailLanguage language;


    public EmailGreeting(){}
    public EmailGreeting(String beginGreeting, String endGreeting){
        this.beginGreeting = beginGreeting;
        this.endGreeting = endGreeting;
    }

}
