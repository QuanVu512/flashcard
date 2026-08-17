package com.flashcardapp.controller;

import com.flashcardapp.dto.RegisterRequest;
import com.flashcardapp.helper.security.SecurityUtil;
import com.flashcardapp.service.UserAlreadyExistsException;
import com.flashcardapp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login() {
        if (SecurityUtil.isLoggedIn()) {
            return "redirect:/library";
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        if (SecurityUtil.isLoggedIn()) {
            return "redirect:/library";
        }
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Mật khẩu xác nhận chưa khớp");
        }
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            userService.register(request);
        } catch (UserAlreadyExistsException exception) {
            bindingResult.rejectValue("email", "email.exists", exception.getMessage());
            return "auth/register";
        }
        redirectAttributes.addFlashAttribute("registered", true);
        return "redirect:/login";
    }
}
