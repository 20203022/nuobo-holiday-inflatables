package com.noble.shop.repository;

import com.noble.shop.model.Order;
import com.noble.shop.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserOrderByCreatedAtDesc(User user);
    List<Order> findByStatusOrderByCreatedAtDesc(Order.OrderStatus status);
    List<Order> findAllByOrderByCreatedAtDesc();
    long countByStatus(Order.OrderStatus status);
    Optional<Order> findByOrderNo(String orderNo);
    List<Order> findByStatusAndCreatedAtBefore(Order.OrderStatus status, LocalDateTime before);
}
