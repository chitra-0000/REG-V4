package com.bnpp.regliss.client.email;

import com.bnpp.regliss.batch.service.NodeDetailsSupplier;
import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.entity.EmailLog.SendingStatus;
import com.bnpp.regliss.entity.batch.Heartbeat;
import com.bnpp.regliss.entity.referential.Scope;
import com.bnpp.regliss.repository.EmailGreetingRepository;
import com.bnpp.regliss.repository.EmailLogRepository;
import com.bnpp.regliss.repository.batch.HeartbeatRepository;
import com.bnpp.regliss.service.IEmailClient;
import com.bnpp.regliss.utils.EntityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
// TODO: lines 26-41 (rest of the imports + class header) were not in the photos; reconstructed from usage - verify.
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.temporal.ChronoUnit.MINUTES;

@Slf4j
@Service
public class EmailClient implements IEmailClient {

    @Value("${mail.from}")
    private String mailFrom;

    @Value("${mail.sending.timeout.minutes}")
    private int mailSendingTimeoutMinutes;

    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    private EmailLogRepository emailLogRepo;

    @Autowired
    private EmailGreetingRepository emailGreetingRepo;

    @Autowired
    private HeartbeatRepository heartbeatRepository;

    @Autowired
    private NodeDetailsSupplier nodeDetailsSupplier;

    public static final String EMAIL_LOG="Trying to send email with subject '{}' to '{}':\n{}";

    public static final String XSL_START_BODY = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">" +
            "<xsl:template match=\"/root\">";

    public static final String XSL_START_SUBJECT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+
                                                    "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">"+
                                                    "<xsl:output method=\"text\" encoding=\"UTF-8\"/>"+
                                                    "<xsl:template match=\"/root\">";

    public static final String XSL_END = "</xsl:template>"+
                                         "</xsl:stylesheet>";

    public static final String HTML_BEGIN = "<html><body>";
    public static final String HTML_END = "</body></html>";
    public static final String BODY_SPACE = "<p/><br/><hr/><br/><p/>";

    @Override
    public void sendMail(EmailTemplate emailTemplate, String xml) {
        sendMail(emailTemplate, xml, emailTemplate.getToAddresses(),emailTemplate.getCcAddresses());
    }
    @Override
    public void sendMail(EmailTemplate emailTemplate, String xml, Scope scope){
        sendMail(emailTemplate, xml, emailTemplate.getToAddresses(scope), emailTemplate.getCcAddresses(scope));
    }
    @Override
    public void sendMail(EmailTemplate emailTemplate, String xml, String[] toAddresses, String[] ccAddresses) {
        //this variable is created to truncate xml to length of column PARAMS from EMAIL_LOG table
        EmailLog tentativeEmailLog = new EmailLog(emailTemplate.getType());
        tentativeEmailLog.setParameters(xml);
        EntityUtils.truncateStringFields(tentativeEmailLog);

        if (toAddresses == null || toAddresses.length == 0) {
            log.warn("No recipient email addresses configured. Skipping sending email {}", emailTemplate.getType());
            return;
        }
        MimeMessage message = emailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(toAddresses);

            if (ccAddresses != null) {
                helper.setCc(ccAddresses);
            }

            String subject = computeEmailPart(xml, XSL_START_SUBJECT + emailTemplate.getSubjectTemplateEN() + XSL_END);
            String body = computeEmailPart(xml, getXlsBody(emailTemplate));

            helper.setSubject(MimeUtility.encodeText(subject));
            helper.setText(body, true);
            if (isEmailAlreadySent(emailTemplate.getType(), tentativeEmailLog.getParameters())) {
                log.debug("Email with subject '{}' was already sent to '{}':\n{}", subject, toAddresses, body);
            } else if (isEmailStillSendingAndTimoutNotOccurred(emailTemplate.getType(), tentativeEmailLog.getParameters())) {
                log.debug("Email with subject '{}' is still being sent to '{}' by another node.", subject, toAddresses);
            } else {
                /* this variable will either be
                    - an existing email with status SENDING_FAILED,
                    - an existing email with status SENDING but timeout between now and creationDate has passed
                    - OR a new EmailLog entry
                 */
                EmailLog emailLog = emailLogRepo.findByTypeAndParameters(emailTemplate.getType(), tentativeEmailLog.getParameters())
                        .orElse(tentativeEmailLog);

                sendAndStoreEmail(toAddresses, ccAddresses, tentativeEmailLog, message, subject, body, emailLog);
            }
        } catch (MessagingException | TransformerException | UnsupportedEncodingException | MailException e ) {
            emailLogRepo
                    .findByTypeAndParameters(emailTemplate.getType(), tentativeEmailLog.getParameters())
                    .ifPresent(emailLog -> updateEmailLogStatus(emailLog.getId(), SendingStatus.SENDING_FAILED));

            log.error("Could not send email {}: {}\nStack is printed on TRACE level", emailTemplate.getType(), e.getMessage());
            log.trace("Exception trace: ", e);
        }
    }

    private void sendAndStoreEmail(String[] toAddresses, String[] ccAddresses, EmailLog tentativeEmailLog, MimeMessage message,
                                   String subject, String body, EmailLog emailLog) {
        try{
            saveEmailSendingAttemptLog(emailLog, toAddresses, ccAddresses);
            log.info( EMAIL_LOG, subject, toAddresses, body);
            emailSender.send(message);
            updateEmailLogStatus(emailLog.getId(), SendingStatus.SENT);
            log.debug("Sending of mail with subject '{}' to '{}' was successful.", subject, toAddresses);
        } catch (DataIntegrityViolationException e) {
            log.info("Email with type {} and parameters {} is already sent. Will skip sending duplicate email.", tentativeEmailLog.getType(),
                    tentativeEmailLog.getParameters());
            log.trace("Exception log: ", e);
        }
    }

    public boolean isEmailAlreadySent(EmailType type, String xml) {
        return !type.equals(EmailType.VERSION_VALIDATION_OK) && !emailLogRepo.findByTypeAndParametersAndSendingStatus(type, xml,
                SendingStatus.SENT).isEmpty();
    }

    @Override
    public void sendMail(EmailTemplate emailTemplate, String xml, String[] toAddresses, String[] ccAddresses, File fileToAttach,
                         String fileName) {
        //this variable is created to truncate xml to length of column PARAMS from EMAIL_LOG table
        EmailLog tentativeEmailLog = new EmailLog(emailTemplate.getType());
        tentativeEmailLog.setParameters(xml);
        EntityUtils.truncateStringFields(tentativeEmailLog);

        if (toAddresses == null || toAddresses.length == 0) {
            log.warn("No recipient email addresses configured. Skipping sending email {}", emailTemplate.getType());
            return;
        }
        MimeMessage message = emailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(toAddresses);

            if (ccAddresses != null) {
                helper.setCc(ccAddresses);
            }

            if(fileToAttach != null) {
                helper.addAttachment(fileName, fileToAttach);
            }

            String subject = computeEmailPart(xml, XSL_START_SUBJECT + emailTemplate.getSubjectTemplateEN() + XSL_END);
            String body = computeEmailPart(xml, getXlsBody(emailTemplate));

            helper.setSubject(MimeUtility.encodeText(subject));
            helper.setText(body, true);
            if (isEmailAlreadySent(emailTemplate.getType(), tentativeEmailLog.getParameters())) {
                log.debug("Email with subject '{}' was already sent to '{}':\n{}", subject, toAddresses, body);
            } else if (isEmailStillSendingAndTimoutNotOccurred(emailTemplate.getType(), tentativeEmailLog.getParameters())) {
                log.debug("Email with subject '{}' is still being sent to '{}' by another node.", subject, toAddresses);
            } else {
                /* this variable will either be
                    - an existing email with status SENDING_FAILED,
                    - an existing email with status SENDING but timeout between now and creationDate has passed
                    - OR a new EmailLog entry
                 */
                EmailLog emailLog = emailLogRepo.findByTypeAndParameters(emailTemplate.getType(), tentativeEmailLog.getParameters())
                        .orElse(tentativeEmailLog);

                sendAndStoreEmail(toAddresses, ccAddresses, tentativeEmailLog, message, subject, body, emailLog);
            }
        } catch (MessagingException | TransformerException | UnsupportedEncodingException | MailException e ) {
            emailLogRepo
                    .findByTypeAndParameters(emailTemplate.getType(), tentativeEmailLog.getParameters())
                    .ifPresent(emailLog -> updateEmailLogStatus(emailLog.getId(), SendingStatus.SENDING_FAILED));

            log.error("Could not send email {}: {}\nStack is printed on TRACE level", emailTemplate.getType(), e.getMessage());
            log.trace("Exception trace: ", e);
        }

    }

    private boolean isEmailStillSendingAndTimoutNotOccurred(EmailType type, String xml) {
        List<EmailLog> result = emailLogRepo.findByTypeAndParametersAndSendingStatus(type, xml, SendingStatus.SENDING);
        return result.stream()
                .anyMatch(emailLog -> MINUTES.between(emailLog.getCreationDate(), LocalDateTime.now()) < mailSendingTimeoutMinutes);

    }

    private void saveEmailSendingAttemptLog(EmailLog emailLog, String[] toAddresses, String[] ccAddresses) {
        emailLog.setCreationDate(LocalDateTime.now());
        emailLog.setToAddress(String.join(",", toAddresses));
        emailLog.setCcAddress(ccAddresses==null?null:String.join(",", ccAddresses));
        emailLog.setSendingStatus(SendingStatus.SENDING);

        emailLogRepo.saveAndFlush(emailLog);
    }

    private void updateEmailLogStatus(Long emailLogId, SendingStatus sendingStatus) {
        EmailLog emailLog = emailLogRepo.getExactlyOne(emailLogId);
        emailLog.setSendingStatus(sendingStatus);
        emailLogRepo.save(emailLog);
    }

    private String computeEmailPart(String xml, String xsl) throws TransformerException {
        TransformerFactory tFactory = TransformerFactory.newInstance();
        tFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        tFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        tFactory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
        tFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        tFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        tFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        tFactory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "");
        tFactory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalStylesheet", "");
        Source xmlSource = new StreamSource(new ByteArrayInputStream(xml.getBytes(UTF_8)));
        Source xslSource = new StreamSource(new ByteArrayInputStream(xsl.getBytes(UTF_8)));
        Transformer transformer = tFactory.newTransformer(xslSource);
        int bufferSize = 3000; //must be able to retain all generated content
        ByteArrayOutputStream outputTarget = new ByteArrayOutputStream(bufferSize);
        transformer.transform(xmlSource, new StreamResult(outputTarget));
        return (outputTarget.toString().replace("&lt;", "<")).replace("&gt;", ">");
    }

    private String getXlsBody(EmailTemplate emailTemplate){
        EmailGreeting emailGreetingFR = emailGreetingRepo.findByLanguage(EmailLanguage.FR);
        EmailGreeting emailGreetingEN = emailGreetingRepo.findByLanguage(EmailLanguage.EN);
        return new StringBuilder()
                .append(XSL_START_BODY)
                .append(HTML_BEGIN)
                .append(emailGreetingEN.getBeginGreeting())
                .append(emailTemplate.getBodyTemplateEN())
                .append(emailGreetingEN.getEndGreeting())
                .append(BODY_SPACE)
                .append(emailGreetingFR.getBeginGreeting())
                .append(emailTemplate.getBodyTemplateFR())
                .append(emailGreetingFR.getEndGreeting())
                .append(HTML_END)
                .append(XSL_END)
                .toString();
    }

}
