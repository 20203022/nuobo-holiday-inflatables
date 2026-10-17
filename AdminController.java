package com.noble.shop.controller.admin;

import com.noble.shop.model.Order.OrderStatus;
import com.noble.shop.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final OrderService orderService;
    private final ProductService productService;
    private final ContactService contactService;
    private final UserService userService;
    private final AnalyticsService analyticsService;

    public AdminController(OrderService orderService, ProductService productService,
                           ContactService contactService, UserService userService, AnalyticsService analyticsService) {
        this.orderService = orderService;
        this.productService = productService;
        this.contactService = contactService;
        this.userService = userService;
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("todayOrders", orderService.findByStatus(OrderStatus.PAID).size());
        model.addAttribute("totalOrders", orderService.countAll());
        model.addAttribute("pendingOrders", orderService.countByStatus(OrderStatus.PENDING_PAYMENT));
        model.addAttribute("totalUsers", userService.countUsers());
        model.addAttribute("todayLeads", contactService.countToday());
        model.addAttribute("newLeads", contactService.countNew());
        model.addAttribute("overdueLeads", contactService.countOverdueNew());
        model.addAttribute("hotProducts", productService.adminFindAll().stream()
            .sorted((a, b) -> Integer.compare(b.getSalesCount(), a.getSalesCount()))
            .limit(6)
            .toList());
        model.addAttribute("recentOrders", orderService.findAll().stream().limit(10).toList());
        model.addAttribute("recentLeads", contactService.findAll().stream().limit(8).toList());
        model.addAttribute("products", productService.adminFindAll().stream()
            .filter(p -> p.getStock() < 10)
            .limit(12)
            .toList());
        model.addAttribute("today", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        model.addAttribute("overdueBefore", LocalDateTime.now().minusHours(24));
        model.addAttribute("analyticsToday", analyticsService.todaySummary());
        model.addAttribute("incompleteProducts", productService.adminFindAll().stream()
            .filter(product -> infoCompletenessScore(product) < 83)
            .sorted((left, right) -> Integer.compare(infoCompletenessScore(left), infoCompletenessScore(right)))
            .limit(8)
            .toList());
        return "admin/dashboard";
    }

    private int infoCompletenessScore(com.noble.shop.model.Product product) {
        int present = 0;
        if (product.getNameEn() != null && !product.getNameEn().isBlank()) present++;
        if (product.getDescriptionEn() != null && !product.getDescriptionEn().isBlank()) present++;
        if (product.getSizeInfo() != null && !product.getSizeInfo().isBlank()) present++;
        if (product.getFeatureTagsEn() != null && !product.getFeatureTagsEn().isBlank()) present++;
        if (product.getImages() != null && !product.getImages().isEmpty()) present++;
        if (product.getOriginalPrice() > 0) present++;
        return Math.round(present * 100f / 6f);
    }
}
