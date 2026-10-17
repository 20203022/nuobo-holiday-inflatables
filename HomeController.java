package com.noble.shop.controller;

import com.noble.shop.model.Product;
import com.noble.shop.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Controller
public class HomeController {

    private final ProductService productService;

    public HomeController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/")
    public String index(Model model) {
        List<Product> newArrivals = productService.getAllPublished();
        List<Product> bestSellers = productService.getBestSellers();
        model.addAttribute("newArrivals", newArrivals.size() > 8 ? newArrivals.subList(0, 8) : newArrivals);
        model.addAttribute("bestSellers", bestSellers.size() > 8 ? bestSellers.subList(0, 8) : bestSellers);

        // Countdown to next major holiday
        LocalDate today = LocalDate.now();
        LocalDate christmas = LocalDate.of(today.getYear(), Month.DECEMBER, 25);
        LocalDate halloween = LocalDate.of(today.getYear(), Month.OCTOBER, 31);
        if (today.isAfter(christmas)) christmas = christmas.plusYears(1);
        if (today.isAfter(halloween)) halloween = halloween.plusYears(1);

        long daysToChristmas = ChronoUnit.DAYS.between(today, christmas);
        long daysToHalloween = ChronoUnit.DAYS.between(today, halloween);

        model.addAttribute("daysToChristmas", daysToChristmas);
        model.addAttribute("daysToHalloween", daysToHalloween);
        model.addAttribute("seoDescription", "Nuobo is a China-based manufacturer of Christmas and Halloween inflatables for retailers, importers, event buyers, and online sellers. Explore ready-to-order models, OEM support, and wholesale inquiries.");
        model.addAttribute("canonicalUrl", "https://nuobo.store/");
        model.addAttribute("seoImage", "https://nuobo.store/images/company/festival-collections-catalog.png");
        model.addAttribute("currentPage", "home");
        return "index";
    }
}
