package ml.dev.mail.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import ml.dev.common.dto.MediaDTO;
import ml.dev.common.dto.config.AccountDTO;
import ml.dev.common.dto.mail.MailDTO;
import ml.dev.common.rest.client.AccountClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Properties;
import java.util.logging.Logger;

@Service
public class EmailService {

    private static final Logger logger = Logger.getLogger(EmailService.class.getName());
    @SuppressWarnings("unused")
    private final ConfigProperties configProperties;
    private final AccountClient accountClient;

    public EmailService(ConfigProperties configProperties) {
        this.configProperties = configProperties;
        this.accountClient = new AccountClient(configProperties.getAccountBaseurl()
                , configProperties.getAccountBasePath()
                , configProperties.getAccountUser()
                , configProperties.getAccountPasswd());
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


}
