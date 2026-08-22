package com.flashcardapp.service;

import com.flashcardapp.config.AuthMailProperties;
import com.flashcardapp.entity.OtpPurpose;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class OtpMailService {

    private final JavaMailSender mailSender;
    private final AuthMailProperties properties;

    public OtpMailService(JavaMailSender mailSender, AuthMailProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    public void send(String recipient, String code, OtpPurpose purpose, long expirationMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.requiredFrom());
        message.setTo(recipient);
        message.setSubject(purpose == OtpPurpose.EMAIL_VERIFICATION
                ? "Xác minh email Flashcard"
                : "Mã đăng nhập Flashcard");
        message.setText("""
                Mã OTP của bạn là: %s

                Mã có hiệu lực trong %d phút và chỉ dùng được một lần.
                Nếu bạn không yêu cầu mã này, hãy bỏ qua email.
                """.formatted(code, expirationMinutes));
        mailSender.send(message);
    }
}
