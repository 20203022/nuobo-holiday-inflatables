package com.noble.shop.service;

import com.noble.shop.model.Product;
import com.noble.shop.model.Product.Category;
import com.noble.shop.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    @Value("${app.upload-dir:src/main/resources/static/uploads}")
    private String uploadDir;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAllPublished() {
        return productRepository.findByPublishedTrueOrderByCreatedAtDesc();
    }

    public Page<Product> getAllPublished(Pageable pageable) {
        return productRepository.findByPublishedTrue(pageable);
    }

    public List<Product> getByCategory(Category category) {
        return productRepository.findByCategoryAndPublishedTrueOrderByCreatedAtDesc(category);
    }

    public Page<Product> getByCategory(Category category, Pageable pageable) {
        return productRepository.findByCategoryAndPublishedTrue(category, pageable);
    }

    public List<Product> getBestSellers() {
        return productRepository.findByPublishedTrueOrderBySalesCountDesc();
    }

    public List<Product> getRelatedProducts(Product product) {
        return productRepository.findTop4ByCategoryAndPublishedTrueAndIdNotOrderBySalesCountDesc(
            product.getCategory(), product.getId());
    }

    public List<Product> search(String query) {
        return productRepository.search(query == null ? "" : query.trim());
    }

    public Page<Product> search(String query, Pageable pageable) {
        return productRepository.search(query == null ? "" : query.trim(), pageable);
    }

    public Page<Product> filterCatalog(Category category, String query, String height, String availability, String buyerNeed, Pageable pageable) {
        String normalizedQuery = normalize(query);
        String normalizedHeight = normalize(height);
        String normalizedAvailability = normalize(availability);
        String normalizedBuyerNeed = normalize(buyerNeed);

        List<Product> filtered = getAllPublished().stream()
            .filter(product -> category == null || product.getCategory() == category)
            .filter(product -> normalizedQuery.isBlank() || matchesQuery(product, normalizedQuery))
            .filter(product -> normalizedHeight.isBlank() || matchesHeight(product, normalizedHeight))
            .filter(product -> normalizedAvailability.isBlank() || matchesAvailability(product, normalizedAvailability))
            .filter(product -> normalizedBuyerNeed.isBlank() || matchesBuyerNeed(product, normalizedBuyerNeed))
            .sorted(Comparator.comparing(Product::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();

        int start = (int) Math.min(pageable.getOffset(), filtered.size());
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
    }

    public List<Product> suggestCatalog(String query, int limit) {
        String normalized = normalize(query);
        if (normalized.isBlank()) return List.of();
        return getAllPublished().stream()
            .filter(product -> matchesQuery(product, normalized))
            .sorted(Comparator.comparing(Product::getSalesCount).reversed()
                .thenComparing(Product::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(Math.max(1, Math.min(limit, 10)))
            .toList();
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    public Optional<Product> findByIdWithImages(Long id) {
        return productRepository.findByIdWithImages(id);
    }

    public List<Product> adminFindAll() {
        return productRepository.findAll();
    }

    @Transactional
    public Product save(Product product) {
        return productRepository.save(product);
    }

    @Transactional
    public void delete(Long id) {
        productRepository.deleteById(id);
    }

    @Transactional
    public String saveImage(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) throw new RuntimeException("Empty image file");
            String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
            String ext = switch (contentType) {
                case "image/jpeg" -> ".jpg";
                case "image/png" -> ".png";
                case "image/webp" -> ".webp";
                default -> throw new RuntimeException("Only JPG, PNG and WEBP images are allowed");
            };
            Path uploadPath = Paths.get(uploadDir, "products");
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            String filename = UUID.randomUUID() + ext;
            Path filePath = uploadPath.resolve(filename);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/products/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save image", e);
        }
    }

    private boolean matchesQuery(Product product, String query) {
        return contains(product.getNameEn(), query)
            || contains(product.getNameCn(), query)
            || contains(product.getModel(), query)
            || contains(product.getFeatureTagsEn(), query)
            || contains(product.getDescriptionEn(), query)
            || contains(product.getSizeInfo(), query);
    }

    private boolean matchesHeight(Product product, String height) {
        double meters = largestMeterValue(product.getSizeInfo());
        if (meters <= 0) return false;
        return switch (height) {
            case "under-2m" -> meters < 2.0;
            case "2-3m" -> meters >= 2.0 && meters < 3.0;
            case "3m-plus" -> meters >= 3.0;
            default -> true;
        };
    }

    private boolean matchesAvailability(Product product, String availability) {
        return switch (availability) {
            case "in-stock" -> product.getStock() > 0;
            case "low-stock" -> product.getStock() > 0 && product.getStock() <= 10;
            case "custom-order" -> product.getStock() == 0;
            default -> true;
        };
    }

    private boolean matchesBuyerNeed(Product product, String buyerNeed) {
        String haystack = normalize(String.join(" ",
            nullToEmpty(product.getNameEn()),
            nullToEmpty(product.getModel()),
            nullToEmpty(product.getFeatureTagsEn()),
            nullToEmpty(product.getDescriptionEn()),
            nullToEmpty(product.getSizeInfo())));
        return switch (buyerNeed) {
            case "motion" -> haystack.contains("move") || haystack.contains("rises") || haystack.contains("rotating") || haystack.contains("animated");
            case "retail-display" -> haystack.contains("christmas") || haystack.contains("santa") || haystack.contains("snowman") || haystack.contains("reindeer");
            case "event-scene" -> haystack.contains("halloween") || haystack.contains("ghost") || haystack.contains("pumpkin") || haystack.contains("arch");
            case "compact" -> largestMeterValue(product.getSizeInfo()) > 0 && largestMeterValue(product.getSizeInfo()) < 2.0;
            default -> true;
        };
    }

    private double largestMeterValue(String text) {
        if (text == null) return 0;
        Matcher matcher = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*M", Pattern.CASE_INSENSITIVE).matcher(text);
        double max = 0;
        while (matcher.find()) {
            max = Math.max(max, Double.parseDouble(matcher.group(1)));
        }
        return max;
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
