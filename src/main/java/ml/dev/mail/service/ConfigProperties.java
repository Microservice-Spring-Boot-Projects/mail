package ml.dev.mail.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "api")
public class ConfigProperties {

    private String accountBaseurl;

    private String accountUser;

    private String accountPasswd;

    private String accountBasePath;

    public String getAccountBaseurl() {
        return accountBaseurl;
    }

    public void setAccountBaseurl(String accountBaseurl) {
        this.accountBaseurl = accountBaseurl;
    }

    public String getAccountUser() {
        return accountUser;
    }

    public void setAccountUser(String accountUser) {
        this.accountUser = accountUser;
    }

    public String getAccountPasswd() {
        return accountPasswd;
    }

    public void setAccountPasswd(String accountPasswd) {
        this.accountPasswd = accountPasswd;
    }

    public String getAccountBasePath() {
        return accountBasePath;
    }

    public void setAccountBasePath(String accountBasePath) {
        this.accountBasePath = accountBasePath;
    }
}
