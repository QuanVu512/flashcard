package com.flashcardapp.controller;

import com.flashcardapp.dto.admin.AdminDashboardResponse;
import com.flashcardapp.dto.admin.AdminUserDetail;
import com.flashcardapp.dto.admin.UserStatusRequest;
import com.flashcardapp.service.AdminService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public AdminDashboardResponse dashboard() {
        return new AdminDashboardResponse(adminService.stats(), adminService.users());
    }

    @GetMapping("/users/{id}")
    public AdminUserDetail userDetail(@PathVariable UUID id) {
        return adminService.userDetail(id);
    }

    @PatchMapping("/users/{id}/status")
    public AdminUserDetail updateUserStatus(@PathVariable UUID id,
                                            @RequestBody UserStatusRequest request,
                                            Authentication authentication) {
        adminService.setEnabled(id, request.enabled(), authentication.getName());
        return adminService.userDetail(id);
    }
}
