package com.noble.shop.controller;

import com.noble.shop.model.User;
import com.noble.shop.service.AuthService;
import com.noble.shop.service.EmailService;
import com.noble.shop.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AuthController(UserService userService, AuthService authService,
                          PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userService = userService;
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("currentPage", "login");
        model.addAttribute("robots", "noindex,follow");
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("currentPage", "register");
        model.addAttribute("robots", "noindex,follow");
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(@RequestParam String name,
                             @RequestParam String email,
                             @RequestParam String password,
                             HttpServletRequest request, HttpServletResponse response,
                             RedirectAttributes ra) {
        name = name.trim();
        email = email.trim().toLowerCase(java.util.Locale.ROOT);
        if (name.isBlank() || name.length() > 200 || email.length() > 254
            || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
            || password.length() < 8 || password.length() > 64) {
            ra.addFlashAttribute("error", "Enter your name, a valid email, and a password of 8 to 64 characters.");
            return "redirect:/register";
        }
        if (userService.findByEmail(email).isPresent()) {
            ra.addFlashAttribute("error", "Email already registered");
            return "redirect:/register";
        }
        User user;
        try {
            user = userService.register(name, email, "", password);
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error", "Unable to create this account. Try logging in or use another email.");
            return "redirect:/register";
        }
        emailService.sendWelcome(email, name);
        // Auto login
        var auth = new UsernamePasswordAuthenticationToken(
            user.getEmail(), null, java.util.List.of(new SimpleGrantedAuthority("ROLE_USER")));
        if (request.getSession(false) != null) request.changeSessionId();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);
        return "redirect:/";
    }

    @PostMapping("/send-code")
    @ResponseBody
    public String sendCode(@RequestParam String email) {
        try {
            authService.sendVerificationCode(email);
            return "ok";
        } catch (RuntimeException e) {
            return "error:" + e.getMessage();
        }
    }

    @GetMapping("/forgot-password")
    public String forgotPassword(Model model) {
        model.addAttribute("robots", "noindex,follow");
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String doForgotPassword(@RequestParam String email, RedirectAttributes ra) {
        if (userService.findByEmail(email).isPresent()) {
            authService.sendPasswordResetToken(email);
        }
        ra.addFlashAttribute("success", "If the email exists, a reset link has been sent");
        return "redirect:/login";
    }

    @GetMapping("/reset-password")
    public String resetPassword(@RequestParam String token, Model model) {
        model.addAttribute("robots", "noindex,follow");
        if (!authService.validateResetToken(token)) {
            model.addAttribute("error", "Invalid or expired reset token");
            return "login";
        }
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String doResetPassword(@RequestParam String token,
                                  @RequestParam String password,
                                  RedirectAttributes ra) {
        if (!authService.validateResetToken(token)) {
            ra.addFlashAttribute("error", "Invalid or expired reset token");
            return "redirect:/login";
        }
        if (password.length() < 8 || password.length() > 64) {
            ra.addFlashAttribute("error", "Use a password of 8 to 64 characters.");
            return "redirect:/reset-password?token=" + java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8);
        }
        String email = authService.getEmailByResetToken(token);
        if (email == null) {
            ra.addFlashAttribute("error", "Invalid token");
            return "redirect:/login";
        }
        User user = userService.findByEmail(email).orElseThrow();
        userService.changePassword(user.getId(), password);
        authService.markResetTokenUsed(token);
        ra.addFlashAttribute("success", "Password reset successfully");
        return "redirect:/login";
    }
}
