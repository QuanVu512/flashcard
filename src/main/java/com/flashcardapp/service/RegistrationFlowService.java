package com.flashcardapp.service;

import com.flashcardapp.dto.RegisterRequest;
import com.flashcardapp.entity.PendingRegistration;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class RegistrationFlowService {

    private final PendingRegistrationService pendingRegistrationService;
    private final OtpService otpService;

    public RegistrationFlowService(PendingRegistrationService pendingRegistrationService,
                                   OtpService otpService) {
        this.pendingRegistrationService = pendingRegistrationService;
        this.otpService = otpService;
    }

    public OtpService.OtpDispatch begin(RegisterRequest request, HttpServletRequest servletRequest) {
        PendingRegistration registration = pendingRegistrationService.create(request);
        try {
            return otpService.dispatchRegistration(registration, servletRequest);
        } catch (RuntimeException exception) {
            pendingRegistrationService.cancel(registration.getId());
            throw exception;
        }
    }
}
