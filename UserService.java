package com.noble.shop.service;

import com.noble.shop.model.User;
import com.noble.shop.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())
            .password(user.getPassword())
            .roles(user.isAdmin() ? "ADMIN" : "USER")
            .disabled(!user.isActive())
            .build();
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public User register(String name, String email, String phone, String password) {
        User user = User.builder()
            .name(name)
            .email(email)
            .phone(phone)
            .password(passwordEncoder.encode(password))
            .emailVerified(false)
            .build();
        return userRepository.save(user);
    }

    @Transactional
    public User quickRegister(String name, String email) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        String displayName = name == null || name.trim().isEmpty() ? normalizedEmail : name.trim();
        User user = User.builder()
            .name(displayName)
            .email(normalizedEmail)
            .phone("")
            .password(passwordEncoder.encode(UUID.randomUUID().toString()))
            .emailVerified(false)
            .build();
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(Long userId, String newPassword) {
        User user = findById(userId);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void updateUser(User user) {
        userRepository.save(user);
    }

    public long countUsers() {
        return userRepository.count();
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional
    public void setActive(Long userId, boolean active) {
        User user = findById(userId);
        user.setActive(active);
        userRepository.save(user);
    }

    @Transactional
    public void setAdmin(Long userId, boolean admin) {
        User user = findById(userId);
        user.setAdmin(admin);
        userRepository.save(user);
    }

    /** Initialize admin account if not exists */
    @Transactional
    public void initAdminAccount() {
        String adminEmail = System.getenv().getOrDefault("NUOBO_ADMIN_EMAIL", "***@***.***");
        String adminPassword = System.getenv().getOrDefault("NUOBO_ADMIN_PASSWORD", "***");
        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = User.builder()
                .name("Admin")
                .email(adminEmail)
                .phone("0000000000")
                .password(passwordEncoder.encode(adminPassword))
                .emailVerified(true)
                .admin(true)
                .build();
            userRepository.save(admin);
        }
    }
}
