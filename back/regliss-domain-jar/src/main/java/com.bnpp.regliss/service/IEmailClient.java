package com.bnpp.regliss.service;

// TODO: [GAP - imports not captured in screenshots: import region was collapsed/folded in editor]

public interface IEmailClient {

    void sendMail(EmailTemplate emailTemplate, String xml);
    void sendMail(EmailTemplate emailTemplate, String xml, Scope scope);

    void sendMail(EmailTemplate emailTemplate, String xml, String[] toAdresses, String[] ccAdresses);

    boolean isEmailAlreadySent(EmailType type, String xml);

    void sendMail(EmailTemplate emailTemplate, String xml, String[] toAdresses, String[] ccAdresses, File fileToAttach, String fileName);
}
