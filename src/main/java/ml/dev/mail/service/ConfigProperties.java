package ml.dev.mail.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;
import lombok.NoArgsConstructor;

@Configuration
@ConfigurationProperties(prefix = "api")
@Data
@NoArgsConstructor
public class ConfigProperties {

    private String accountBaseurl;

    private String accountUser;

    private String accountPasswd;

    private String accountBasePath;

}
