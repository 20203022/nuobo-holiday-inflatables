package com.noble.shop.service;

import com.noble.shop.model.ContactMessage;
import com.noble.shop.model.Product;
import com.noble.shop.repository.ContactMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ContactService {

    private final ContactMessageRepository contactRepo;
    private final EmailService emailService;

    public ContactService(ContactMessageRepository contactRepo, EmailService emailService) {
        this.contactRepo = contactRepo;
        this.emailService = emailService;
    }

    @Transactional
    public void submitContact(String name, String email, String message,
                              boolean isInquiry, String productModel, Integer quantity) {
        submitContact(name, email, null, null, null, message, isInquiry, productModel, quantity);
    }

    @Transactional
    public void submitContact(String name, String email, String company, String country, String phone,
                              String message, boolean isInquiry, String productModel, Integer quantity) {
        submitContact(name, email, company, country, phone, message, isInquiry, productModel, quantity, "CONTACT");
    }

    @Transactional
    public void submitContact(String name, String email, String company, String country, String phone,
                              String message, boolean isInquiry, String productModel, Integer quantity, String source) {
        submitContact(name, email, company, country, phone, message, isInquiry, null, null, productModel, quantity, null, source);
    }

    @Transactional
    public void submitProductInquiry(String name, String email, String company, String country, String phone,
                                     String message, Product product, Integer quantity, String pageUrl, String source) {
        submitContact(name, email, company, country, phone, message, true,
            product == null ? null : product.getId(),
            product == null ? null : product.getNameEn(),
            product == null ? null : product.getModel(),
            quantity, pageUrl, source);
    }

    @Transactional
    public void submitContact(String name, String email, String company, String country, String phone,
                              String message, boolean isInquiry, Long productId, String productName,
                              String productModel, Integer quantity, String pageUrl, String source) {
        ContactMessage cm = ContactMessage.builder()
            .name(name)
            .email(email)
            .company(company)
            .country(country)
            .phone(phone)
            .message(message)
            .productInquiry(isInquiry)
            .productId(productId)
            .productName(productName)
            .productModel(productModel)
            .inquiryQuantity(quantity)
            .pageUrl(pageUrl)
            .source(source == null || source.isBlank() ? "CONTACT" : source)
            .status("NEW")
            .build();
        contactRepo.save(cm);
        String detail = "Company: " + safe(company) + "<br>Country: " + safe(country) + "<br>Phone: " + safe(phone) +
            "<br>Product: " + safe(productModel) + "<br>Product name: " + safe(productName) +
            "<br>Page: " + safe(pageUrl) + "<br>Quantity: " + (quantity == null ? "" : quantity) +
            "<br><br>" + message;
        emailService.sendContactNotification(name, email, detail);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public List<ContactMessage> findAll() {
        return contactRepo.findAllByOrderByCreatedAtDesc();
    }

    public List<ContactMessage> findByStatus(String status) {
        if (status == null || status.isBlank()) return findAll();
        return contactRepo.findByStatusOrderByCreatedAtDesc(status);
    }

    public List<ContactMessage> findByStatusAndQuery(String status, String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        return findByStatus(status).stream().filter(message -> normalized.isBlank()
            || contains(message.getName(), normalized) || contains(message.getEmail(), normalized)
            || contains(message.getCompany(), normalized) || contains(message.getCountry(), normalized)
            || contains(message.getProductModel(), normalized) || contains(message.getProductName(), normalized)
            || contains(message.getMessage(), normalized)).toList();
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(java.util.Locale.ROOT).contains(query);
    }

    public long countToday() {
        return contactRepo.countByCreatedAtAfter(LocalDateTime.now().toLocalDate().atStartOfDay());
    }

    public long countNew() {
        return contactRepo.countByStatus("NEW");
    }

    public long countOverdueNew() {
        return contactRepo.countByStatusAndCreatedAtBefore("NEW", LocalDateTime.now().minusHours(24));
    }

    @Transactional
    public void markRead(Long id) {
        contactRepo.findById(id).ifPresent(msg -> {
            msg.setRead(true);
            contactRepo.save(msg);
        });
    }

    @Transactional
    public void updateStatus(Long id, String status, String note) {
        contactRepo.findById(id).ifPresent(msg -> {
            msg.setStatus(status == null || status.isBlank() ? "NEW" : status);
            msg.setInternalNote(note);
            if (!"NEW".equals(msg.getStatus())) msg.setRead(true);
            contactRepo.save(msg);
        });
    }

    @Transactional
    public void delete(Long id) {
        contactRepo.deleteById(id);
    }
}
