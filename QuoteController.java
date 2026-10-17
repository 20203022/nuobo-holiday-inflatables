package com.noble.shop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/quote-list")
public class QuoteController {

    @GetMapping
    public String redirectToContact() {
        return "redirect:/contact";
    }
}
