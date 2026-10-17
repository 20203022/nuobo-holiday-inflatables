package com.noble.shop.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.noble.shop.model.*;
import com.noble.shop.service.*;
import com.noble.shop.service.OrderService.CartItem;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private final OrderService orderService;
    private final CouponService couponService;
    private final AlipayClient alipayClient;
    private final UserService userService;

    @Value("${alipay.return-url}")
    private String alipayReturnUrl;

    @Value("${alipay.notify-url}")
    private String alipayNotifyUrl;

    @Value("${alipay.alipay-public-key}")
    private String alipayPublicKey;

    public CheckoutController(OrderService orderService, CouponService couponService, AlipayClient alipayClient, UserService userService) {
        this.orderService = orderService;
        this.couponService = couponService;
        this.alipayClient = alipayClient;
        this.userService = userService;
    }

    @ModelAttribute
    public void addSeoMetadata(Model model) {
        model.addAttribute("robots", "noindex,follow");
    }

    @GetMapping
    public String checkout(Model model, HttpSession session) {
        @SuppressWarnings("unchecked")
        var cart = (List<CartItem>) session.getAttribute("cart");
        cart = refreshCart(cart, session);
        if (cart == null || cart.isEmpty()) return "redirect:/cart";
        double subtotal = cart.stream().mapToDouble(CartItem::getSubtotal).sum();
        Coupon coupon = null;
        String code = (String) session.getAttribute("couponCode");
        if (code != null) coupon = couponService.findByCode(code).orElse(null);
        double discount = coupon != null ? couponService.calculateDiscount(coupon, subtotal) : 0;
        double shippingFee = orderService.calculateShippingFee(subtotal, coupon);
        model.addAttribute("cartItems", cart);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("discount", discount);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("total", subtotal - discount + shippingFee);
        model.addAttribute("couponCode", code);
        return "checkout";
    }

    @PostMapping("/apply-coupon")
    public String applyCoupon(@RequestParam String code, HttpSession session, org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        var coupon = couponService.findByCode(code);
        if (coupon.isEmpty() || !coupon.get().isValid()) {
            ra.addFlashAttribute("couponError", "Invalid or expired coupon");
        } else {
            session.setAttribute("couponCode", code);
            ra.addFlashAttribute("couponSuccess", "Coupon applied!");
        }
        return "redirect:/checkout";
    }

    @PostMapping("/pay")
    @ResponseBody
    public String createPayment(@RequestParam String name, @RequestParam String phone,
                                @RequestParam String address, @RequestParam String city,
                                @RequestParam String state, @RequestParam String zip,
                                @RequestParam String country,
                                HttpSession session) {
        return "error:Online payment is not enabled yet. Please contact sales for this order.";
        /*
        @SuppressWarnings("unchecked")
        var cart = (List<CartItem>) session.getAttribute("cart");
        cart = refreshCart(cart, session);
        if (cart == null || cart.isEmpty()) return "error:cart empty";

        // Get current user from database
        String email = org.springframework.security.core.context.SecurityContextHolder
            .getContext().getAuthentication().getName();
        User user = userService.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));

        double subtotal = cart.stream().mapToDouble(CartItem::getSubtotal).sum();

        // Apply coupon
        Coupon coupon = null;
        String code = (String) session.getAttribute("couponCode");
        if (code != null) coupon = couponService.findByCode(code).orElse(null);
        double discount = coupon != null ? couponService.calculateDiscount(coupon, subtotal) : 0;

        // Create order in DB (pending payment)
        Order order = orderService.createOrder(user, cart, name, phone, address, city, state, zip, country,
            coupon, subtotal, discount);

        // Build Alipay payment page request
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(alipayReturnUrl + "?orderNo=" + order.getOrderNo());
        alipayRequest.setNotifyUrl(alipayNotifyUrl);
        alipayRequest.setBizContent("{" +
            "\"out_trade_no\":\"" + order.getOrderNo() + "\"," +
            "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"," +
            "\"total_amount\":\"" + String.format("%.2f", order.getTotal()) + "\"," +
            "\"subject\":\"nuobo Order " + order.getOrderNo() + "\"" +
            "}");

        try {
            String form = alipayClient.pageExecute(alipayRequest).getBody();
            // Store Alipay form in session for redirect
            session.setAttribute("alipayForm", form);
            session.setAttribute("pendingOrderNo", order.getOrderNo());
            return "/checkout/alipay-redirect";
        } catch (AlipayApiException e) {
            return "error:payment creation failed - " + e.getMessage();
        }
        */
    }

    @GetMapping("/alipay-redirect")
    public String alipayRedirect(HttpSession session, Model model) {
        String form = (String) session.getAttribute("alipayForm");
        if (form == null) return "redirect:/checkout";
        model.addAttribute("alipayForm", form);
        return "alipay-redirect";
    }

    @GetMapping("/success")
    public String success(@RequestParam(required = false) String orderNo,
                          @RequestParam(required = false) String trade_no,
                          @RequestParam(required = false) String out_trade_no,
                          @RequestParam(required = false) String total_amount,
                          HttpSession session, Model model) {
        if (orderNo == null) {
            orderNo = out_trade_no != null ? out_trade_no : (String) session.getAttribute("pendingOrderNo");
        }

        if (orderNo == null) return "redirect:/";

        var optOrder = orderService.findByOrderNo(orderNo);
        if (optOrder.isEmpty()) return "redirect:/";

        Order order = optOrder.get();
        String email = org.springframework.security.core.context.SecurityContextHolder
            .getContext().getAuthentication().getName();
        if (!order.getUser().getEmail().equals(email)) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Order not found");
        }

        if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
            model.addAttribute("orderNo", order.getOrderNo());
            session.removeAttribute("cart");
            session.removeAttribute("couponCode");
            session.removeAttribute("alipayForm");
            session.removeAttribute("pendingOrderNo");
            return "order-success";
        }

        session.removeAttribute("alipayForm");
        session.removeAttribute("pendingOrderNo");

        model.addAttribute("orderNo", order.getOrderNo());
        model.addAttribute("pendingPayment", true);
        return "order-success";
    }

    @PostMapping("/notify")
    @ResponseBody
    public String notify(HttpServletRequest request) {
        try {
            // Verify Alipay signature
            Map<String, String> params = new HashMap<>();
            Map<String, String[]> requestParams = request.getParameterMap();
            for (String name : requestParams.keySet()) {
                String[] values = requestParams.get(name);
                StringBuilder valueStr = new StringBuilder();
                for (int i = 0; i < values.length; i++) {
                    valueStr.append(i == 0 ? "" : ",").append(values[i]);
                }
                params.put(name, valueStr.toString());
            }

            boolean signVerified = AlipaySignature.rsaCheckV1(
                params, alipayPublicKey, "UTF-8", "RSA2");

            if (!signVerified) return "failure";

            String tradeStatus = request.getParameter("trade_status");
            String tradeNo = request.getParameter("trade_no");
            String outTradeNo = request.getParameter("out_trade_no");
            String totalAmount = request.getParameter("total_amount");

            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                var optOrder = orderService.findByOrderNo(outTradeNo);
                if (optOrder.isPresent()) {
                    Order order = optOrder.get();
                    if (!sameMoney(order.getTotal(), totalAmount)) return "failure";
                    if (order.getPaymentStatus() != Order.PaymentStatus.PAID) {
                        orderService.markPaid(order, tradeNo);
                    }
                }
            }

            return "success";
        } catch (Exception e) {
            return "failure";
        }
    }

    @GetMapping("/cancel")
    public String cancel() {
        return "redirect:/cart";
    }

    private boolean sameMoney(double expected, String actual) {
        try {
            BigDecimal left = BigDecimal.valueOf(expected).setScale(2, RoundingMode.HALF_UP);
            BigDecimal right = new BigDecimal(actual).setScale(2, RoundingMode.HALF_UP);
            return left.compareTo(right) == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private List<CartItem> refreshCart(List<CartItem> cart, HttpSession session) {
        if (cart == null) return null;
        cart.removeIf(item -> {
            Long productId = item.getProduct() == null ? null : item.getProduct().getId();
            if (productId == null || item.getQuantity() <= 0) return true;
            var latest = orderService.findProductForCart(productId);
            if (latest.isEmpty() || !latest.get().isPublished() || latest.get().getStock() <= 0) return true;
            item.setProduct(latest.get());
            item.setQuantity(Math.min(item.getQuantity(), latest.get().getStock()));
            return false;
        });
        session.setAttribute("cart", cart);
        return cart;
    }
}
