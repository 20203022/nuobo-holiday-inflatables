package com.noble.shop.service;

import com.noble.shop.model.*;
import com.noble.shop.model.Order.OrderStatus;
import com.noble.shop.model.Order.PaymentStatus;
import com.noble.shop.repository.OrderItemRepository;
import com.noble.shop.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductService productService;
    private final EmailService emailService;
    private final CouponService couponService;

    @Value("${app.default-shipping-fee}")
    private double defaultShippingFee;

    @Value("${app.free-shipping-threshold}")
    private double freeShippingThreshold;

    @Value("${app.order-auto-cancel-minutes:30}")
    private long autoCancelMinutes;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository, ProductService productService,
                        EmailService emailService, CouponService couponService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productService = productService;
        this.emailService = emailService;
        this.couponService = couponService;
    }

    public String generateOrderNo() {
        String date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "NUOBO-" + date + "-" + System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    @Transactional
    public Order createOrder(User user, List<CartItem> items, String name, String phone,
                             String address, String city, String state, String zip, String country,
                             Coupon coupon, double subtotal, double discount) {
        for (CartItem item : items) {
            Product latest = productService.findById(item.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("Product not found"));
            if (!latest.isPublished()) throw new RuntimeException("Product is unavailable: " + latest.getModel());
            if (item.getQuantity() <= 0 || item.getQuantity() > latest.getStock()) {
                throw new RuntimeException("Insufficient stock for " + latest.getModel());
            }
            item.setProduct(latest);
        }

        Order order = Order.builder()
            .orderNo(generateUniqueOrderNo())
            .user(user)
            .shippingName(name)
            .shippingPhone(phone)
            .shippingAddress((country == null || country.isBlank() ? "" : country.trim() + " · ") + address)
            .shippingCity(city)
            .shippingState(state)
            .shippingZip(zip)
            .subtotal(subtotal)
            .discountAmount(discount)
            .shippingFee(calculateShippingFee(subtotal, coupon))
            .total(subtotal - discount + calculateShippingFee(subtotal, coupon))
            .couponCode(coupon != null ? coupon.getCode() : null)
            .build();
        order = orderRepository.save(order);

        for (CartItem item : items) {
            Product p = item.getProduct();
            String image = (p.getImages() != null && !p.getImages().isEmpty()) ? p.getImages().get(0).getImagePath() : null;
            orderItemRepository.save(OrderItem.builder()
                .order(order)
                .product(p)
                .productName(p.getNameEn())
                .productModel(p.getModel())
                .unitPrice(p.getDisplayPrice())
                .quantity(item.getQuantity())
                .subtotal(roundMoney(p.getDisplayPrice() * item.getQuantity()))
                .productImage(image)
                .build());
        }

        emailService.sendNewOrderNotification(order.getOrderNo(), order.getTotal(), user.getName());
        return order;
    }

    private String generateUniqueOrderNo() {
        String orderNo;
        do {
            orderNo = generateOrderNo();
        } while (orderRepository.findByOrderNo(orderNo).isPresent());
        return orderNo;
    }

    public double calculateShippingFee(double subtotal, Coupon coupon) {
        if (subtotal >= freeShippingThreshold) return 0;
        if (coupon != null && coupon.getType() == Coupon.CouponType.FREE_SHIPPING && coupon.isValid()) return 0;
        return defaultShippingFee;
    }

    @Transactional
    public void markPaid(Order order, String tradeNo) {
        if (order.getPaymentStatus() == PaymentStatus.PAID) return;
        for (OrderItem item : getItems(order.getId())) {
            Product p = productService.findById(item.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("Product not found"));
            if (item.getQuantity() > p.getStock()) {
                throw new RuntimeException("Insufficient stock for paid order " + order.getOrderNo());
            }
            p.setStock(p.getStock() - item.getQuantity());
            p.setSalesCount(p.getSalesCount() + item.getQuantity());
            productService.save(p);
        }
        if (order.getCouponCode() != null && !order.getCouponCode().isBlank()) {
            couponService.findByCode(order.getCouponCode()).ifPresent(couponService::useCoupon);
        }
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        order.setTradeNo(tradeNo);
        orderRepository.save(order);
        emailService.sendOrderConfirmation(order.getUser().getEmail(), order.getOrderNo(), order.getTotal());
    }

    @Transactional
    public void ship(Order order, String company, String trackingNo) {
        order.setStatus(OrderStatus.SHIPPED);
        order.setShippedAt(LocalDateTime.now());
        order.setTrackingCompany(company);
        order.setTrackingNumber(trackingNo);
        orderRepository.save(order);
        emailService.sendShippingNotification(order.getUser().getEmail(), order.getOrderNo(), company, trackingNo);
    }

    @Transactional
    public void cancel(Order order) {
        if (order.getStatus() == OrderStatus.CANCELLED) return;
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        orderRepository.save(order);
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            for (OrderItem item : getItems(order.getId())) {
                Product p = item.getProduct();
                if (p == null) continue;
                p.setStock(p.getStock() + item.getQuantity());
                p.setSalesCount(Math.max(0, p.getSalesCount() - item.getQuantity()));
                productService.save(p);
            }
        }
    }

    @Transactional
    public void refund(Order order) {
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new IllegalStateException("Only a paid order can be refunded");
        }
        if (order.getStatus() == OrderStatus.REFUNDED) return;
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        order.setStatus(OrderStatus.REFUNDED);
        orderRepository.save(order);
    }

    public List<Order> findByUser(User user) {
        return orderRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    public Optional<Order> findByOrderNo(String orderNo) {
        return orderRepository.findByOrderNo(orderNo);
    }

    public List<Order> findAll() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Order> findByStatus(OrderStatus status) {
        return orderRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void cancelExpiredPendingOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(autoCancelMinutes);
        for (Order order : orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING_PAYMENT, cutoff)) {
            cancel(order);
        }
    }

    public long countByStatus(OrderStatus status) {
        return orderRepository.countByStatus(status);
    }

    public long countAll() {
        return orderRepository.count();
    }

    @Transactional
    public Order save(Order order) {
        return orderRepository.save(order);
    }

    public List<OrderItem> getItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    public Optional<Product> findProductForCart(Long productId) {
        return productService.findByIdWithImages(productId);
    }

    @Transactional
    public void updateShipping(Order order, String name, String phone,
                                String address, String city, String state, String zip) {
        order.setShippingName(name);
        order.setShippingPhone(phone);
        order.setShippingAddress(address);
        order.setShippingCity(city);
        order.setShippingState(state);
        order.setShippingZip(zip);
        orderRepository.save(order);
    }

    public static class CartItem {
        private Product product;
        private int quantity;

        public CartItem(Product product, int quantity) {
            this.product = product;
            this.quantity = quantity;
        }

        public Product getProduct() { return product; }
        public void setProduct(Product product) { this.product = product; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public double getSubtotal() { return product.getDisplayPrice() * quantity; }
    }

    private double roundMoney(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
