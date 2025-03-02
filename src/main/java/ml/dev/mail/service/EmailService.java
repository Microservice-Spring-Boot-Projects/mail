package ml.dev.mail.service;

import jakarta.mail.BodyPart;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.BodyTerm;
import jakarta.mail.search.DateTerm;
import jakarta.mail.search.FromStringTerm;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.SearchTerm;
import jakarta.mail.search.SubjectTerm;
import ml.dev.common.dto.MediaDTO;
import ml.dev.common.dto.config.AccountDTO;
import ml.dev.common.dto.mail.MailDTO;
import ml.dev.common.exception.ExceptionCodes;
import ml.dev.common.exception.MLException;
import ml.dev.common.rest.client.AccountClient;
import ml.dev.mail.dto.MailSearchRequest;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import io.micrometer.common.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class EmailService {

    private static final Logger logger = Logger.getLogger(EmailService.class.getName());
    @SuppressWarnings("unused")
    private final ConfigProperties configProperties;
    private final AccountClient accountClient;

    public EmailService(ConfigProperties configProperties) {
        this.configProperties = configProperties;
        this.accountClient = new AccountClient(configProperties.getAccountBaseurl(),
                configProperties.getAccountBasePath(), configProperties.getAccountUser(),
                configProperties.getAccountPasswd());
    }

    public List<MailDTO> getMails(MailSearchRequest msr) throws MLException {
        Store store = getImapsConnection(msr.getAccountId());
        try {
            Folder inbox = store.getFolder("inbox");
            inbox.open(Folder.READ_ONLY);
            Calendar cal = Calendar.getInstance(Locale.GERMAN);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.add(Calendar.DAY_OF_MONTH, -1);
            List<SearchTerm> lSearchTerms = new ArrayList<>();
            lSearchTerms.add(new ReceivedDateTerm(DateTerm.GT, cal.getTime()));
            if (msr.getFromSearchTerm() != null && !msr.getFromSearchTerm().isEmpty())
                lSearchTerms.add(new FromStringTerm(msr.getFromSearchTerm()));
            if (msr.getSubjectSearchTerm() != null && !msr.getSubjectSearchTerm().isEmpty())
                lSearchTerms.add(new SubjectTerm(msr.getSubjectSearchTerm()));
            if (msr.getBodySearchTerm() != null && !msr.getBodySearchTerm().isEmpty())
                lSearchTerms.add(new BodyTerm(msr.getBodySearchTerm()));
            Message[] messages = inbox.search(new AndTerm(lSearchTerms.toArray(new SearchTerm[0])));
            return Arrays.asList(messages).stream().map(message -> mapperDTO(message)).collect(Collectors.toList());
        } catch (MessagingException e) {
            throw new MLException(ExceptionCodes.MAIL_SERVER_FAILED, e);
        }
    }

    private MailDTO mapperDTO(Message message) {
        try {
            MailDTO mailDTO = new MailDTO();
            mailDTO.setSubject(message.getSubject());
            if (message.isMimeType("text/plain"))
                mailDTO.setText(message.getContent().toString());
            if (message.isMimeType("multipart/*")) {
                MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
                mailDTO.setText(getTextFromMimeMultipart(mimeMultipart));
            }
            mailDTO.setTos(Arrays.asList(message.getRecipients(Message.RecipientType.TO)).stream()
                    .map(address -> address.toString())
                    .collect(Collectors.toList()));
            if (message.getRecipients(Message.RecipientType.CC) != null)
                mailDTO.setCcs(Arrays.asList(message.getRecipients(Message.RecipientType.CC)).stream()
                        .map(address -> address.toString()).collect(Collectors.toList()));

            mailDTO.setFrom(((InternetAddress) message.getFrom()[0]).getAddress());
            mailDTO.setMedias(getAttachmentFiles(message));
            return mailDTO;
        } catch (MessagingException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Store getImapsConnection(String accountId) throws MLException {
        AccountDTO accountDTO = accountClient.getAccountData("mail", accountId);
        Properties props = System.getProperties();
        props.setProperty("mail.store.protocol", "imaps");
        Session session = Session.getDefaultInstance(props, null);
        try {
            Store store = session.getStore("imaps");
            store.connect(accountDTO.getProperty("mail.imap.host", "mail").getPropertyValue(),
                    accountDTO.getProperty("mail.sender.username", "mail").getPropertyValue(),
                    accountDTO.getProperty("mail.sender.password", "mail").getPropertyValue());
            return store;
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new MLException(ExceptionCodes.MAIL_SERVER_FAILED, e);
        }
    }

    public void sendMail(MailDTO mailDTO, String accountId) {
        JavaMailSender jms = getJavaMailSender(accountId);
        MimeMessage message = jms.createMimeMessage();
        try {
            MimeMessageHelper helperMsg = new MimeMessageHelper(message, true);
            helperMsg.setFrom(mailDTO.getFrom());
            if (mailDTO.getReplyTo() != null)
                helperMsg.setReplyTo(mailDTO.getReplyTo());
            helperMsg.setTo(String.join(",", mailDTO.getTos()));
            if (mailDTO.getBccs() != null && !mailDTO.getBccs().isEmpty())
                helperMsg.setBcc(String.join(",", mailDTO.getBccs()));
            if (mailDTO.getCcs() != null && !mailDTO.getCcs().isEmpty())
                helperMsg.setCc(String.join(",", mailDTO.getCcs()));
            helperMsg.setSubject(mailDTO.getSubject());
            helperMsg.setText(mailDTO.getText(), true);
            if (!mailDTO.getMedias().isEmpty())
                for (MediaDTO mediaDTO : mailDTO.getMedias())
                    helperMsg.addAttachment(mediaDTO.getName(), new ByteArrayResource(mediaDTO.getContent()));
            jms.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
            logger.info(e.getMessage());
        }
    }

    public JavaMailSender getJavaMailSender(String accountId) {
        AccountDTO accountDTO = accountClient.getAccountData("mail", accountId);
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(accountDTO.getProperty("mail.sender.host", "mail").getPropertyValue());
        mailSender.setPort(Integer.valueOf(accountDTO.getProperty("mail.sender.port", "mail").getPropertyValue()));

        mailSender.setUsername(accountDTO.getProperty("mail.sender.username", "mail").getPropertyValue());
        mailSender.setPassword(accountDTO.getProperty("mail.sender.password", "mail").getPropertyValue());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", "false");
        props.put("mail.imap.ssl.trust", "*");
        props.put("mail.smtp.ssl.trust", "*");
        props.put("mail.smtp.ssl.checkserveridentity", "false");
        props.put("debug", "true");
        return mailSender;
    }

    private String getTextFromMimeMultipart(
            MimeMultipart mimeMultipart) throws MessagingException, IOException {
        String result = "";
        for (int i = 0; i < mimeMultipart.getCount(); i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                return result + "\n" + bodyPart.getContent(); // without return, same text appears twice in my tests
            }
            result += this.parseBodyPart(bodyPart);
        }
        return result;
    }

    private String parseBodyPart(BodyPart bodyPart) throws MessagingException, IOException {
        if (bodyPart.isMimeType("text/html"))
            return "\n" + org.jsoup.Jsoup
                    .parse(bodyPart.getContent().toString())
                    .text();
        if (bodyPart.getContent() instanceof MimeMultipart)
            return getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent());
        return "";
    }

    private List<MediaDTO> getAttachmentFiles(Message msg) {
        List<MediaDTO> attachments = new ArrayList<>();
        try {
            Multipart multipart = (Multipart) msg.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if (!Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) &&
                        StringUtils.isBlank(bodyPart.getFileName())) {
                    continue; // dealing with attachments only
                }
                InputStream is = bodyPart.getInputStream();
                byte[] bytes = IOUtils.toByteArray(is);
                attachments.add(new MediaDTO(bodyPart.getContentType(), bytes, bodyPart.getFileName()));
                is.close();
            }
        } catch (MessagingException | IOException e) {
            e.printStackTrace();
        }
        return attachments;
    }

}