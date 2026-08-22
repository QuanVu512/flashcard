package com.flashcardapp.service;

import com.flashcardapp.config.AuthMailProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class OtpMailWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(OtpMailWorker.class);

    private final OtpMailQueueService queueService;
    private final OtpMailService mailService;
    private final AuthMailProperties properties;
    private final Executor executor;
    private final AtomicInteger inFlight = new AtomicInteger();

    public OtpMailWorker(OtpMailQueueService queueService,
                         OtpMailService mailService,
                         AuthMailProperties properties,
                         @Qualifier("otpMailExecutor") Executor executor) {
        this.queueService = queueService;
        this.mailService = mailService;
        this.properties = properties;
        this.executor = executor;
    }

    @Scheduled(fixedDelayString = "${app.auth.mail.worker-delay-ms:500}")
    public void submitAvailable() {
        if (!properties.isEnabled()) {
            return;
        }
        int concurrency = properties.workerConcurrencyOrDefault();
        while (inFlight.get() < concurrency) {
            Optional<OtpMailQueueService.ClaimedOtpMail> claimed = queueService.claimNext();
            if (claimed.isEmpty()) {
                return;
            }
            inFlight.incrementAndGet();
            submit(claimed.get());
        }
    }

    private void submit(OtpMailQueueService.ClaimedOtpMail mail) {
        try {
            executor.execute(() -> deliver(mail));
        } catch (RuntimeException exception) {
            inFlight.decrementAndGet();
            queueService.markFailed(mail.deliveryId(), exception);
            LOGGER.error("OTP mail executor rejected delivery {}", mail.deliveryId(), exception);
        }
    }

    private void deliver(OtpMailQueueService.ClaimedOtpMail mail) {
        try {
            mailService.send(mail.recipient(), mail.code(), mail.purpose(), mail.expirationMinutes());
            queueService.markSent(mail.deliveryId());
        } catch (Exception exception) {
            queueService.markFailed(mail.deliveryId(), exception);
            LOGGER.warn("OTP mail delivery {} failed", mail.deliveryId(), exception);
        } finally {
            inFlight.decrementAndGet();
        }
    }
}
