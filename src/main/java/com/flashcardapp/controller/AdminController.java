package com.flashcardapp.controller;

import com.flashcardapp.service.AdminService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("stats", adminService.stats());
        model.addAttribute("users", adminService.users());
        return "admin/dashboard";
    }

    @GetMapping("/admin/users/{id}")
    public String userDetail(@PathVariable UUID id, Model model) {
        model.addAttribute("managedUser", adminService.userDetail(id));
        return "admin/user-detail";
    }

    @PostMapping("/admin/users/{id}/status")
    public String updateUserStatus(@PathVariable UUID id,
                                   @RequestParam boolean enabled,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            adminService.setEnabled(id, enabled, authentication.getName());
        } catch (AccessDeniedException exception) {
            redirectAttributes.addFlashAttribute("adminError", exception.getMessage());
            return "redirect:/admin/users/" + id;
        }
        redirectAttributes.addFlashAttribute("adminMessage", enabled ? "Đã mở khóa tài khoản." : "Đã khóa tài khoản.");
        return "redirect:/admin/users/" + id;
    }
}
