package ml.dev.mail.service;

import ml.dev.common.dto.config.AccountDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class AccountService {

    private final WebClient webClient;

    public AccountService(ConfigProperties configProperties, WebClient webClient) {
        this.webClient = webClient.mutate()
                .baseUrl(configProperties.getAccountBaseurl() + "/" + configProperties.getAccountBasePath())
                .build();
    }

    public AccountDTO getAccountData(String type, String identifier) {
        return webClient.get().uri(uriBuilder -> uriBuilder.path("/account").path("/read")
                .queryParam("type", type).queryParam("identifier", identifier).build()).retrieve()
                .bodyToMono(AccountDTO.class).block();
    }

}
