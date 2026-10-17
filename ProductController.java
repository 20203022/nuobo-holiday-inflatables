package com.noble.shop.controller;

import com.noble.shop.model.Product;
import com.noble.shop.model.Product.Category;
import com.noble.shop.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.MediaType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ProductController {

    private static final int CATALOG_PAGE_SIZE = 12;

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public String allProducts(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(required = false) String q,
                              @RequestParam(required = false) String category,
                              @RequestParam(required = false) String height,
                              @RequestParam(required = false) String availability,
                              @RequestParam(required = false) String buyerNeed,
                              Model model) {
        Category cat = parseCategory(category);
        Page<Product> productPage = productService.filterCatalog(cat, q, height, availability, buyerNeed,
            PageRequest.of(safePage(page), CATALOG_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt")));
        addProductPage(model, productPage, "/products", q, category, height, availability, buyerNeed);
        model.addAttribute("categoryName", cat == null ? "Wholesale Inflatable Catalog" : displayCategory(cat));
        model.addAttribute("currentPage", "products");
        model.addAttribute("seoTitle", cat == null
            ? "Wholesale Christmas & Halloween Inflatables | Nuobo Catalog"
            : "Wholesale " + (cat == Category.CHRISTMAS ? "Christmas" : "Halloween") + " Inflatables for Retail & Events | Nuobo");
        model.addAttribute("seoDescription", "Browse wholesale Christmas and Halloween inflatables by model, height, availability, and buyer use. Request MOQ, packing, OEM, lead time, and shipping details.");
        model.addAttribute("canonicalUrl", "https://nuobo.store/products");
        if (page > 0 || hasCatalogFilters(q, category, height, availability, buyerNeed)) {
            model.addAttribute("robots", "noindex,follow");
        }
        return "product-list";
    }

    @GetMapping("/category/{category}")
    public String list(@PathVariable String category,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) String q,
                       @RequestParam(required = false) String height,
                       @RequestParam(required = false) String availability,
                       @RequestParam(required = false) String buyerNeed,
                       Model model) {
        Category cat = parseCategory(category);
        if (cat == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
        Page<Product> productPage = productService.filterCatalog(cat, q, height, availability, buyerNeed,
            PageRequest.of(safePage(page), CATALOG_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt")));
        addProductPage(model, productPage, "/category/" + (category.equalsIgnoreCase("christmas") ? "christmas" : "halloween"),
            q, null, height, availability, buyerNeed);
        model.addAttribute("categoryName", displayCategory(cat));
        model.addAttribute("currentPage", category.equalsIgnoreCase("christmas") ? "christmas" : "halloween");
        model.addAttribute("seoTitle", "Wholesale " + (cat == Category.CHRISTMAS ? "Christmas" : "Halloween") + " Inflatables for Retail & Events | Nuobo");
        model.addAttribute("seoDescription", "Wholesale " + (cat == Category.CHRISTMAS ? "Christmas" : "Halloween") + " inflatables for importers, retailers, events, and holiday display buyers. Compare models and request bulk pricing.");
        model.addAttribute("canonicalUrl", "https://nuobo.store/category/" + (category.equalsIgnoreCase("christmas") ? "christmas" : "halloween"));
        if (page > 0 || hasCatalogFilters(q, null, height, availability, buyerNeed)) {
            model.addAttribute("robots", "noindex,follow");
        }
        return "product-list";
    }

    @GetMapping("/product/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Product product = productService.findById(id)
            .filter(Product::isPublished)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        model.addAttribute("product", product);
        model.addAttribute("relatedProducts", productService.getRelatedProducts(product));
        model.addAttribute("seoDescription", productSeoDescription(product));
        model.addAttribute("seoTitle", productSeoTitle(product));
        model.addAttribute("canonicalUrl", "https://nuobo.store/product/" + product.getId());
        model.addAttribute("seoImage", product.getImages() != null && !product.getImages().isEmpty()
            ? "https://nuobo.store" + product.getImages().get(0).getImagePath()
            : "https://nuobo.store/images/company/festival-collections-catalog.png");
        model.addAttribute("ogType", "product");
        model.addAttribute("currentPage", "product");
        return "product-detail";
    }

    @GetMapping("/search")
    public String search(@RequestParam(defaultValue = "") String q,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(required = false) String category,
                         @RequestParam(required = false) String height,
                         @RequestParam(required = false) String availability,
                         @RequestParam(required = false) String buyerNeed,
                         Model model) {
        Category cat = parseCategory(category);
        Page<Product> productPage = productService.filterCatalog(cat, q, height, availability, buyerNeed,
            PageRequest.of(safePage(page), CATALOG_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt")));
        addProductPage(model, productPage, "/search", q, category, height, availability, buyerNeed);
        model.addAttribute("query", q);
        model.addAttribute("currentPage", "search");
        model.addAttribute("categoryName", "Product Search");
        model.addAttribute("seoTitle", "Search Wholesale Holiday Inflatables | Nuobo");
        model.addAttribute("seoDescription", "Search nuobo wholesale holiday inflatables by model, holiday, size, stock, and buying need.");
        model.addAttribute("canonicalUrl", "https://nuobo.store/search");
        model.addAttribute("robots", "noindex,follow");
        return "product-list";
    }

    @GetMapping(value = "/api/search-suggestions", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Map<String, Object>> searchSuggestions(@RequestParam(defaultValue = "") String q) {
        return productService.suggestCatalog(q, 6).stream().map(product -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", product.getId());
            result.put("model", product.getModel());
            result.put("name", product.getNameEn());
            result.put("category", product.getCategory().name().toLowerCase(java.util.Locale.ROOT));
            result.put("url", "/product/" + product.getId());
            return result;
        }).toList();
    }

    private int safePage(int page) {
        return Math.max(0, page);
    }

    private boolean hasCatalogFilters(String query, String category, String height, String availability, String buyerNeed) {
        return (query != null && !query.isBlank())
            || (category != null && !category.isBlank())
            || (height != null && !height.isBlank())
            || (availability != null && !availability.isBlank())
            || (buyerNeed != null && !buyerNeed.isBlank());
    }

    private String productSeoTitle(Product product) {
        String title = product.getSeoTitle() != null && !product.getSeoTitle().isBlank()
            ? product.getSeoTitle().trim()
            : product.getModel() + " " + product.getNameEn();
        title = title.replaceAll("(?i)\\s*\\|\\s*nuobo\\s*$", "").trim();
        if (!title.toLowerCase(java.util.Locale.ROOT).contains("wholesale")) {
            title += " | Wholesale";
        }
        return title + " | Nuobo";
    }

    private String productSeoDescription(Product product) {
        String description = product.getSeoDescription() != null && !product.getSeoDescription().isBlank()
            ? product.getSeoDescription().trim()
            : product.getModel() + " " + product.getNameEn()
                + " for wholesale holiday inflatable buyers. Request MOQ, packing, OEM, lead time, and shipping details.";
        return description.replaceAll("(?i)\\bseasonal\\b", "holiday");
    }

    private void addProductPage(Model model, Page<Product> productPage, String pageBaseUrl, String query,
                                String category, String height, String availability, String buyerNeed) {
        model.addAttribute("productPage", productPage);
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("totalProducts", productPage.getTotalElements());
        model.addAttribute("currentProductPage", productPage.getNumber());
        model.addAttribute("totalProductPages", productPage.getTotalPages());
        model.addAttribute("hasNextProductPage", productPage.hasNext());
        model.addAttribute("hasPreviousProductPage", productPage.hasPrevious());
        model.addAttribute("pageBaseUrl", pageBaseUrl);
        model.addAttribute("pageQuery", query);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedHeight", height);
        model.addAttribute("selectedAvailability", availability);
        model.addAttribute("selectedBuyerNeed", buyerNeed);
        model.addAttribute("pageQueryString", buildPageQueryString(query, category, height, availability, buyerNeed));
    }

    private Category parseCategory(String category) {
        if (category == null || category.isBlank()) return null;
        if (category.equalsIgnoreCase("christmas")) return Category.CHRISTMAS;
        if (category.equalsIgnoreCase("halloween")) return Category.HALLOWEEN;
        return null;
    }

    private String displayCategory(Category category) {
        return category == Category.CHRISTMAS ? "Wholesale Christmas Inflatables" : "Wholesale Halloween Inflatables";
    }

    private String buildPageQueryString(String query, String category, String height, String availability, String buyerNeed) {
        StringBuilder builder = new StringBuilder();
        appendParam(builder, "q", query);
        appendParam(builder, "category", category);
        appendParam(builder, "height", height);
        appendParam(builder, "availability", availability);
        appendParam(builder, "buyerNeed", buyerNeed);
        return builder.toString();
    }

    private void appendParam(StringBuilder builder, String name, String value) {
        if (value == null || value.isBlank()) return;
        builder.append('&').append(name).append('=').append(java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8));
    }
}
