package com.noble.shop.controller;

import com.noble.shop.model.*;
import com.noble.shop.service.*;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class PageController {

    private final ContactService contactService;
    private final ProductService productService;
    private final PageContentService pageContentService;
    private final NewsletterService newsletterService;

    public PageController(ContactService contactService, ProductService productService,
                          PageContentService pageContentService, NewsletterService newsletterService) {
        this.contactService = contactService;
        this.productService = productService;
        this.pageContentService = pageContentService;
        this.newsletterService = newsletterService;
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("content", pageContentService.getContent("about"));
        model.addAttribute("seoDescription", "Learn about nuobo's wholesale holiday inflatable decoration supply, holiday product catalog, export workflow, and OEM support.");
        model.addAttribute("canonicalUrl", "https://nuobo.store/about");
        return "about";
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("products", productService.getAllPublished());
        model.addAttribute("seoDescription", "Request wholesale pricing, MOQ, packaging, OEM, and lead time details for Christmas and Halloween inflatables from nuobo.");
        model.addAttribute("canonicalUrl", "https://nuobo.store/contact");
        return "contact";
    }

    @GetMapping("/inquiry")
    public String inquiry(Model model) {
        model.addAttribute("seoDescription", "Build a multi-product wholesale inquiry for Nuobo Christmas and Halloween inflatables, including quantities, OEM needs, packaging, and target delivery dates.");
        model.addAttribute("canonicalUrl", "https://nuobo.store/inquiry");
        model.addAttribute("robots", "noindex,follow");
        return "inquiry";
    }

    @PostMapping("/contact")
    public String submitContact(@RequestParam String name, @RequestParam String email,
                                @RequestParam(required = false) String company,
                                @RequestParam(required = false) String country,
                                @RequestParam(required = false) String phone,
                                @RequestParam String message,
                                @RequestParam(defaultValue = "false") boolean productInquiry,
                                @RequestParam(required = false) List<Long> productIds,
                                @RequestParam(required = false) List<Integer> productQuantities,
                                @RequestParam(required = false) String productModel,
                                @RequestParam(required = false) Integer inquiryQuantity,
                                @RequestParam(required = false) String targetDate,
                                @RequestParam(required = false) String packaging,
                                @RequestParam(required = false) String oemNeeds,
                                @RequestParam(required = false) String referenceUrl,
                                @RequestParam(required = false) String catalogRequest,
                                @RequestParam(defaultValue = "false") boolean returnToInquiry,
                                RedirectAttributes ra) {
        String enrichedMessage = enrichBuyerMessage(message, targetDate, packaging, oemNeeds, referenceUrl, catalogRequest);
        Map<Long, Integer> requestedQuantities = new LinkedHashMap<>();
        if (productIds != null) {
            for (int index = 0; index < productIds.size(); index++) {
                Long productId = productIds.get(index);
                if (productId == null || requestedQuantities.containsKey(productId)) {
                    continue;
                }
                Integer requestedQuantity = productQuantities != null && index < productQuantities.size()
                    ? productQuantities.get(index)
                    : null;
                requestedQuantities.put(productId, requestedQuantity == null ? 1 : Math.max(1, Math.min(100000, requestedQuantity)));
            }
        }
        List<Product> selectedProducts = requestedQuantities.keySet().stream()
            .distinct()
            .map(productService::findById)
            .flatMap(java.util.Optional::stream)
            .toList();
        String selectedModels = selectedProducts.stream()
            .map(product -> product.getModel() + " - " + product.getNameEn() + " (Qty: " + requestedQuantities.getOrDefault(product.getId(), 1) + ")")
            .reduce((left, right) -> left + "; " + right)
            .orElse(productModel == null ? "" : productModel.trim());
        if (!selectedProducts.isEmpty()) {
            String selectedProductLines = selectedProducts.stream()
                .map(product -> "- " + product.getModel() + " - " + product.getNameEn() + " | Qty: " + requestedQuantities.getOrDefault(product.getId(), 1) + " | https://nuobo.store/product/" + product.getId())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
            enrichedMessage = "Selected products:\n" + selectedProductLines + "\n\n" + enrichedMessage;
        }
        Integer totalQuantity = selectedProducts.isEmpty()
            ? inquiryQuantity
            : selectedProducts.stream().mapToInt(product -> requestedQuantities.getOrDefault(product.getId(), 1)).sum();
        contactService.submitContact(name, email, company, country, phone, enrichedMessage,
            productInquiry || !selectedProducts.isEmpty(), selectedModels, totalQuantity, "CONTACT_MULTI_PRODUCT");
        ra.addFlashAttribute("success", "Message sent successfully!");
        return returnToInquiry ? "redirect:/inquiry?sent" : "redirect:/contact";
    }

    @PostMapping("/quote")
    public String submitQuote(@RequestParam String name,
                              @RequestParam String email,
                              @RequestParam(required = false) String company,
                              @RequestParam(required = false) String country,
                              @RequestParam(required = false) String phone,
                              @RequestParam(required = false) Long productId,
                              @RequestParam String productModel,
                              @RequestParam(defaultValue = "1") Integer inquiryQuantity,
                              @RequestParam(required = false) String pageUrl,
                              @RequestParam(required = false) String targetDate,
                              @RequestParam(required = false) String packaging,
                              @RequestParam(required = false) String oemNeeds,
                              @RequestParam(required = false) String referenceUrl,
                              @RequestParam(required = false) String message,
                              RedirectAttributes ra) {
        String body = (message == null || message.isBlank())
            ? "Product quote request"
            : message;
        body = enrichBuyerMessage(body, targetDate, packaging, oemNeeds, referenceUrl, null);
        var product = productId == null ? null : productService.findById(productId).orElse(null);
        if (product != null) {
            contactService.submitProductInquiry(name, email, company, country, phone, body, product, inquiryQuantity, pageUrl, "PRODUCT_QUOTE");
        } else {
            contactService.submitContact(name, email, company, country, phone, body, true, productModel, inquiryQuantity, "PRODUCT_QUOTE");
        }
        ra.addFlashAttribute("success", "Thanks. Our sales team will reply with product details and pricing.");
        return "redirect:/contact";
    }

    @GetMapping("/faq")
    public String faq(Model model) {
        model.addAttribute("content", pageContentService.getContent("faq"));
        model.addAttribute("seoDescription", "Answers to common wholesale holiday inflatable questions about MOQ, OEM customization, packaging, lead times, shipping, and product selection.");
        model.addAttribute("canonicalUrl", "https://nuobo.store/faq");
        return "faq";
    }

    @GetMapping("/privacy")
    public String privacy(Model model) {
        model.addAttribute("content", pageContentService.getContent("privacy"));
        model.addAttribute("seoDescription", "Nuobo privacy policy for wholesale holiday inflatable inquiries, customer accounts, and website visitors.");
        model.addAttribute("canonicalUrl", "https://nuobo.store/privacy");
        model.addAttribute("robots", "noindex,follow");
        return "privacy";
    }

    @GetMapping("/return-policy")
    public String returnPolicy(Model model) {
        model.addAttribute("content", pageContentService.getContent("return-policy"));
        model.addAttribute("seoDescription", "Nuobo return and after-sales policy for holiday inflatable wholesale orders.");
        model.addAttribute("canonicalUrl", "https://nuobo.store/return-policy");
        model.addAttribute("robots", "noindex,follow");
        return "return-policy";
    }

    @PostMapping("/subscribe")
    public String subscribe(@RequestParam String email, RedirectAttributes ra) {
        newsletterService.subscribe(email);
        ra.addFlashAttribute("success", "Subscribed! Check your email for confirmation.");
        return "redirect:/";
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String sitemap() {
        String today = LocalDate.now().toString();
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        appendSitemapUrl(xml, "https://nuobo.store/", today, "daily", "1.0");
        appendSitemapUrl(xml, "https://nuobo.store/products", today, "daily", "0.9");
        appendSitemapUrl(xml, "https://nuobo.store/category/christmas", today, "weekly", "0.8");
        appendSitemapUrl(xml, "https://nuobo.store/category/halloween", today, "weekly", "0.8");
        appendSitemapUrl(xml, "https://nuobo.store/about", today, "monthly", "0.7");
        appendSitemapUrl(xml, "https://nuobo.store/contact", today, "monthly", "0.7");
        productService.getAllPublished().forEach(product ->
            appendSitemapUrl(xml, "https://nuobo.store/product/" + product.getId(), today, "weekly", "0.8"));
        xml.append("</urlset>");
        return xml.toString();
    }

    private void appendSitemapUrl(StringBuilder xml, String loc, String lastmod, String changefreq, String priority) {
        xml.append("  <url><loc>").append(loc).append("</loc><lastmod>").append(lastmod)
            .append("</lastmod><changefreq>").append(changefreq).append("</changefreq><priority>")
            .append(priority).append("</priority></url>\n");
    }

    private String enrichBuyerMessage(String message, String targetDate, String packaging, String oemNeeds, String referenceUrl, String catalogRequest) {
        StringBuilder body = new StringBuilder(message == null ? "" : message.trim());
        appendBuyerField(body, "Target delivery date", targetDate);
        appendBuyerField(body, "Packaging requirements", packaging);
        appendBuyerField(body, "OEM / customization needs", oemNeeds);
        appendBuyerField(body, "Reference URL / company website", referenceUrl);
        appendBuyerField(body, "Catalog request", catalogRequest);
        return body.toString();
    }

    private void appendBuyerField(StringBuilder body, String label, String value) {
        if (value == null || value.isBlank()) return;
        if (body.length() > 0) body.append("\n");
        body.append(label).append(": ").append(value.trim());
    }
}
