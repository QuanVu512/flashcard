package com.flashcardapp.service.mail.mailjet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MailjetSendResponse(
        @JsonProperty("Messages") List<MessageResult> messages
) {

    public void requireSuccess() {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalStateException("Mailjet không trả về trạng thái gửi email");
        }
        MessageResult result = messages.getFirst();
        if (!"success".equalsIgnoreCase(result.status())) {
            throw new IllegalStateException(result.failureMessage());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MessageResult(
            @JsonProperty("Status") String status,
            @JsonProperty("Errors") List<MailjetError> errors
    ) {

        private String failureMessage() {
            if (errors == null || errors.isEmpty()) {
                return "Mailjet từ chối gửi email mà không cung cấp chi tiết";
            }
            String details = errors.stream()
                    .map(MailjetError::displayMessage)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining("; "));
            return details.isBlank()
                    ? "Mailjet từ chối gửi email"
                    : "Mailjet từ chối gửi email: " + details;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MailjetError(
            @JsonProperty("ErrorCode") String code,
            @JsonProperty("ErrorMessage") String message
    ) {

        private String displayMessage() {
            if (StringUtils.hasText(code) && StringUtils.hasText(message)) {
                return code.trim() + " - " + message.trim();
            }
            if (StringUtils.hasText(message)) {
                return message.trim();
            }
            return StringUtils.hasText(code) ? code.trim() : "";
        }
    }
}
