package ml.dev.mail.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MailSearchRequest {

    private String accountId;

    private String fromSearchTerm;

    private String subjectSearchTerm;

    private String bodySearchTerm;

}
