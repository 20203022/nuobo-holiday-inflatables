package com.noble.shop.repository;

import com.noble.shop.model.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
    List<ContactMessage> findAllByOrderByCreatedAtDesc();
    List<ContactMessage> findByStatusOrderByCreatedAtDesc(String status);
    long countByCreatedAtAfter(LocalDateTime since);
    long countByStatus(String status);
    long countByStatusAndCreatedAtBefore(String status, LocalDateTime before);
}
