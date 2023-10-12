package ml.dev.mail.rest;

import ml.dev.common.dto.MediaDTO;
import ml.dev.common.dto.mail.MailDTO;
import ml.dev.common.exception.MLException;
import ml.dev.common.rest.JsonHelper;
import ml.dev.mail.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "send")
public class SenderController {

    private final EmailService emailService;

    @Autowired
    public SenderController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity sendMail(@RequestPart(value = "mail_data") String mailData, @RequestPart(value = "account_id") String accountId) {
        try {
            MailDTO mail = JsonHelper.jsonToObject(MailDTO.class, mailData);
            emailService.sendMail(mail, accountId);
        } catch (MLException e) {
            return ResponseEntity.status(500).build();
        }
        return ResponseEntity.ok(null);
    }

}
