package com.flashcardapp.service.mail.mailjet;

import com.flashcardapp.config.MailjetMailProperties;
import com.flashcardapp.service.mail.OtpMailSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "app.auth.mail.provider", havingValue = "mailjet")
public class MailjetOtpMailSender implements OtpMailSender {

    private static final String SEND_EMAIL_PATH = "/v3.1/send";

    private final MailjetMailProperties properties;
    private final RestClient restClient;

    public MailjetOtpMailSender(MailjetMailProperties properties) {
        this.properties = properties;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.connectTimeoutMsOrDefault()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.readTimeoutMsOrDefault()));
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrlOrDefault())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public void send(String from, String recipient, String subject, String content) {
        MailjetSendRequest request = MailjetSendRequest.single(
                from,
                properties.senderNameOrDefault(),
                recipient,
                subject,
                content
        );

        MailjetSendResponse response = restClient.post()
                .uri(SEND_EMAIL_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBasicAuth(
                        properties.requiredApiKey(),
                        properties.requiredSecretKey()
                ))
                .body(request)
                .retrieve()
                .body(MailjetSendResponse.class);
        if (response == null) {
            throw new IllegalStateException("Mailjet không trả về phản hồi gửi email");
        }
        response.requireSuccess();
    }
}
