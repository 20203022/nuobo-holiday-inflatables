package com.noble.shop.controller;

import com.noble.shop.model.User;
import com.noble.shop.model.Order;
import com.noble.shop.service.OrderService;
import com.noble.shop.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Controller
@RequestMapping("/account")
public class AccountController {

    private final UserService userService;
    private final OrderService orderService;
    private final PasswordEncoder passwordEncoder;

    public AccountController(UserService userService, OrderService orderService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.orderService = orderService;
        this.passwordEncoder = passwordEncoder;
    }

    @ModelAttribute
    public void addSeoMetadata(Model model) {
        model.addAttribute("robots", "noindex,follow");
    }

    private User getUser(Authentication auth) {
        return userService.findByEmail(auth.getName()).orElseThrow();
    }

    @GetMapping
    public String dashboard(Authentication auth, Model model) {
        User user = getUser(auth);
        model.addAttribute("orders", orderService.findByUser(user));
        model.addAttribute("user", user);
        return "account/dashboard";
    }

    @GetMapping("/profile")
    public String profile(Authentication auth, Model model) {
        model.addAttribute("user", getUser(auth));
        return "account/profile";
    }

    @PostMapping("/profile")
    public String saveProfile(Authentication auth,
                              @RequestParam String name,
                              @RequestParam String phone,
                              @RequestParam(required = false) String shippingAddress,
                              @RequestParam(required = false) String shippingCity,
                              @RequestParam(required = false) String shippingState,
                              @RequestParam(required = false) String shippingZip,
                              @RequestParam(required = false) String shippingCountry,
                              @RequestParam(required = false) MultipartFile avatarFile,
                              RedirectAttributes ra) {
        User user = getUser(auth);
        user.setName(name);
        user.setPhone(phone);
        user.setShippingAddress(shippingAddress);
        user.setShippingCity(shippingCity);
        user.setShippingState(shippingState);
        user.setShippingZip(shippingZip);
        user.setShippingCountry(shippingCountry);

        // Handle avatar upload
        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                String uploadDir = "/opt/nuobo-shop/uploads/avatars";
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

                String contentType = avatarFile.getContentType() == null ? "" : avatarFile.getContentType().toLowerCase(Locale.ROOT);
                String ext = switch (contentType) {
                    case "image/jpeg" -> ".jpg";
                    case "image/png" -> ".png";
                    case "image/webp" -> ".webp";
                    default -> throw new IOException("Unsupported avatar image type");
                };
                String filename = UUID.randomUUID() + ext;
                Path filePath = uploadPath.resolve(filename);
                try (InputStream in = avatarFile.getInputStream()) {
                    Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
                user.setAvatar("/uploads/avatars/" + filename);
            } catch (IOException e) {
                ra.addFlashAttribute("error", "Failed to upload avatar");
                return "redirect:/account/profile";
            }
        }

        userService.updateUser(user);
        ra.addFlashAttribute("success", "Profile updated successfully");
        return "redirect:/account/profile";
    }

    @GetMapping("/addresses")
    public String addresses(Authentication auth, Model model) {
        model.addAttribute("user", getUser(auth));
        return "account/addresses";
    }

    @PostMapping("/addresses")
    public String saveAddress(Authentication auth,
                              @RequestParam String shippingName,
                              @RequestParam String shippingPhone,
                              @RequestParam String shippingAddress,
                              @RequestParam String shippingCity,
                              @RequestParam String shippingState,
                              @RequestParam String shippingZip) {
        User user = getUser(auth);
        user.setName(shippingName);
        user.setPhone(shippingPhone);
        user.setShippingAddress(shippingAddress);
        user.setShippingCity(shippingCity);
        user.setShippingState(shippingState);
        user.setShippingZip(shippingZip);
        userService.updateUser(user);
        return "redirect:/account/addresses";
    }

    @GetMapping("/password")
    public String password(Authentication auth, Model model) {
        model.addAttribute("needsFirstPassword", false);
        return "account/password";
    }

    @PostMapping("/password")
    public String updatePassword(Authentication auth,
                                 @RequestParam(required = false) String currentPassword,
                                 @RequestParam String newPassword,
                                 RedirectAttributes ra) {
        User user = getUser(auth);
        if (!passwordEncoder.matches(currentPassword == null ? "" : currentPassword, user.getPassword())) {
            ra.addFlashAttribute("error", "Current password is incorrect");
            return "redirect:/account/password";
        }
        if (newPassword.length() < 8 || newPassword.length() > 64) {
            ra.addFlashAttribute("error", "Use a password of 8 to 64 characters.");
            return "redirect:/account/password";
        }
        userService.changePassword(user.getId(), newPassword);
        ra.addFlashAttribute("success", "Password updated");
        return "redirect:/account/password";
    }

    @GetMapping("/orders")
    public String orders(Authentication auth, Model model) {
        User user = getUser(auth);
        model.addAttribute("orders", orderService.findByUser(user));
        return "account/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, Authentication auth, Model model) {
        User user = getUser(auth);
        Order order = orderService.findById(id).orElseThrow();
        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        model.addAttribute("order", order);
        model.addAttribute("items", orderService.getItems(id));
        return "account/order-detail";
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        User user = getUser(auth);
        Order order = orderService.findById(id).orElseThrow();
        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        if (order.getStatus() != Order.OrderStatus.PENDING_PAYMENT) {
            ra.addFlashAttribute("error", "Order can only be cancelled when pending payment");
            return "redirect:/account/orders/" + id;
        }
        orderService.cancel(order);
        ra.addFlashAttribute("success", "Order cancelled successfully");
        return "redirect:/account/orders";
    }

    @GetMapping("/orders/{id}/edit")
    public String editOrder(@PathVariable Long id, Authentication auth, Model model) {
        User user = getUser(auth);
        Order order = orderService.findById(id).orElseThrow();
        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        if (order.getStatus() != Order.OrderStatus.PENDING_PAYMENT) {
            return "redirect:/account/orders/" + id;
        }
        model.addAttribute("order", order);
        return "account/order-edit";
    }

    @PostMapping("/orders/{id}/edit")
    public String saveOrderEdit(@PathVariable Long id, Authentication auth,
                                 @RequestParam String shippingName,
                                 @RequestParam String shippingPhone,
                                 @RequestParam String shippingAddress,
                                 @RequestParam String shippingCity,
                                 @RequestParam String shippingState,
                                 @RequestParam String shippingZip,
                                 RedirectAttributes ra) {
        User user = getUser(auth);
        Order order = orderService.findById(id).orElseThrow();
        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        if (order.getStatus() != Order.OrderStatus.PENDING_PAYMENT) {
            ra.addFlashAttribute("error", "Order can only be edited when pending payment");
            return "redirect:/account/orders/" + id;
        }
        orderService.updateShipping(order, shippingName, shippingPhone, shippingAddress, shippingCity, shippingState, shippingZip);
        ra.addFlashAttribute("success", "Shipping info updated");
        return "redirect:/account/orders/" + id;
    }
}
