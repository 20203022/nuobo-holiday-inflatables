package com.noble.shop.controller;

import com.noble.shop.service.AnalyticsService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class AnalyticsController {
    private final AnalyticsService analyticsService;
    public AnalyticsController(AnalyticsService analyticsService) { this.analyticsService = analyticsService; }

    @PostMapping("/analytics/event")
    @ResponseBody
    public String event(@RequestParam String event,
                        @RequestParam(required = false) String path,
                        @RequestParam(required = false) Long productId,
                        @RequestParam(required = false) String value) {
        analyticsService.record(event, path, productId, value);
        return "ok";
    }
}
