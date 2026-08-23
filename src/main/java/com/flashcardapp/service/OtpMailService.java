package com.flashcardapp.service;

import com.flashcardapp.config.AuthMailProperties;
import com.flashcardapp.entity.OtpPurpose;
import com.flashcardapp.service.mail.OtpMailSender;
import org.springframework.stereotype.Service;

@Service
public class OtpMailService {

    private final OtpMailSender mailSender;
    private final AuthMailProperties properties;

    public OtpMailService(OtpMailSender mailSender, AuthMailProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    public void send(String recipient, String code, OtpPurpose purpose, long expirationMinutes) {
        String subject = purpose.verifiesEmail()
                ? "Xác minh email Flashcard"
                : "Mã đăng nhập Flashcard";
        String content = """
                Mã OTP của bạn là: %s

                Mã có hiệu lực trong %d phút và chỉ dùng được một lần.
                Nếu bạn không yêu cầu mã này, hãy bỏ qua email.
                """.formatted(code, expirationMinutes);
        mailSender.send(properties.requiredFrom(), recipient, subject, content);
    }
}
