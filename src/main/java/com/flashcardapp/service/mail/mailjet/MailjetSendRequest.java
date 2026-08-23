package com.flashcardapp.service.mail.mailjet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MailjetSendRequest(
        @JsonProperty("Messages") List<Message> messages
) {

    public static MailjetSendRequest single(String from,
                                            String senderName,
                                            String recipient,
                                            String subject,
                                            String content) {
        Message message = new Message(
                new Contact(from, senderName),
                List.of(new Contact(recipient, null)),
                subject,
                content
        );
        return new MailjetSendRequest(List.of(message));
    }

    public record Message(
            @JsonProperty("From") Contact from,
            @JsonProperty("To") List<Contact> to,
            @JsonProperty("Subject") String subject,
            @JsonProperty("TextPart") String textPart
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Contact(
            @JsonProperty("Email") String email,
            @JsonProperty("Name") String name
    ) {
    }
}
