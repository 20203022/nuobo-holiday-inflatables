package com.noble.shop.controller;

import com.noble.shop.model.Product;
import com.noble.shop.service.OrderService.CartItem;
import com.noble.shop.service.ProductService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final ProductService productService;

    public CartController(ProductService productService) {
        this.productService = productService;
    }

    @SuppressWarnings("unchecked")
    private List<CartItem> getCart(HttpSession session) {
        List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
        if (cart == null) {
            cart = new ArrayList<>();
            session.setAttribute("cart", cart);
        }
        return cart;
    }

    private List<CartItem> refreshCart(HttpSession session) {
        var cart = getCart(session);
        cart.removeIf(item -> {
            Long productId = item.getProduct() == null ? null : item.getProduct().getId();
            if (productId == null || item.getQuantity() <= 0) return true;
            var latest = productService.findByIdWithImages(productId);
            if (latest.isEmpty() || !latest.get().isPublished() || latest.get().getStock() <= 0) return true;
            item.setProduct(latest.get());
            item.setQuantity(Math.min(item.getQuantity(), latest.get().getStock()));
            return false;
        });
        return cart;
    }

    @PostMapping("/add")
    public String add(@RequestParam Long productId, @RequestParam(defaultValue = "1") int quantity,
                      HttpSession session) {
        var cart = getCart(session);
        if (quantity < 1) quantity = 1;
        Product p = productService.findByIdWithImages(productId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        if (!p.isPublished() || p.getStock() <= 0) return "redirect:/cart";
        var existing = cart.stream().filter(i -> i.getProduct().getId().equals(productId)).findFirst();
        if (existing.isPresent()) {
            existing.get().setProduct(p);
            existing.get().setQuantity((int) Math.min(p.getStock(), (long) existing.get().getQuantity() + quantity));
        } else {
            cart.add(new CartItem(p, Math.min(p.getStock(), quantity)));
        }
        return "redirect:/cart";
    }

    @PostMapping("/update")
    public String update(@RequestParam Long productId, @RequestParam int quantity, HttpSession session) {
        var cart = getCart(session);
        cart.stream().filter(i -> i.getProduct().getId().equals(productId)).findFirst().ifPresent(i -> {
            if (quantity <= 0) cart.remove(i);
            else {
                productService.findByIdWithImages(productId).ifPresent(p -> {
                    i.setProduct(p);
                    if (!p.isPublished() || p.getStock() <= 0) {
                        cart.remove(i);
                    } else {
                        i.setQuantity(Math.min(quantity, p.getStock()));
                    }
                });
            }
        });
        return "redirect:/cart";
    }

    @PostMapping("/remove")
    public String remove(@RequestParam Long productId, HttpSession session) {
        var cart = getCart(session);
        cart.removeIf(i -> i.getProduct().getId().equals(productId));
        return "redirect:/cart";
    }

    @PostMapping(value = "/update", headers = "X-Requested-With=XMLHttpRequest")
    @ResponseBody
    public Map<String, Object> updateAjax(@RequestParam Long productId, @RequestParam int quantity, HttpSession session) {
        update(productId, quantity, session);
        var item = refreshCart(session).stream().filter(i -> i.getProduct().getId().equals(productId)).findFirst();
        if (item.isEmpty()) return Map.of("removed", true);
        return Map.of("removed", false, "quantity", item.get().getQuantity(),
            "stock", item.get().getProduct().getStock(), "unitPrice", item.get().getProduct().getDisplayPrice());
    }

    @GetMapping
    public String view(Model model, HttpSession session) {
        var cart = refreshCart(session);
        double subtotal = cart.stream().mapToDouble(CartItem::getSubtotal).sum();
        model.addAttribute("cartItems", cart);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("currentPage", "cart");
        model.addAttribute("robots", "noindex,follow");
        return "cart";
    }
}
