package com.flashcardapp.bootstrap;

import com.flashcardapp.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserService userService;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminDisplayName;

    public AdminBootstrap(UserService userService,
                          @Value("${app.admin.email:}") String adminEmail,
                          @Value("${app.admin.password:}") String adminPassword,
                          @Value("${app.admin.display-name:Admin}") String adminDisplayName) {
        this.userService = userService;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.adminDisplayName = adminDisplayName;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail == null || adminEmail.isBlank()) {
            return;
        }
        if (adminPassword == null || adminPassword.length() < 8) {
            throw new IllegalStateException("ADMIN_PASSWORD cần tối thiểu 8 ký tự khi bật tài khoản admin.");
        }
        userService.ensureAdminUser(adminEmail, adminPassword, adminDisplayName);
        LOGGER.info("Admin account bootstrap finished for configured email.");
    }
}
