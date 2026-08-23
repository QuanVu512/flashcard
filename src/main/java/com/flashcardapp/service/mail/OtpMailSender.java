package com.flashcardapp.service.mail;

public interface OtpMailSender {

    void send(String from, String recipient, String subject, String content);
}
