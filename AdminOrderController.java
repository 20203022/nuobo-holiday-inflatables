package com.noble.shop.controller.admin;

import com.noble.shop.model.Order;
import com.noble.shop.model.Order.OrderStatus;
import com.noble.shop.service.EmailService;
import com.noble.shop.service.OrderService;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeRefundRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;
    private final EmailService emailService;
    private final AlipayClient alipayClient;

    public AdminOrderController(OrderService orderService, EmailService emailService, AlipayClient alipayClient) {
        this.orderService = orderService;
        this.emailService = emailService;
        this.alipayClient = alipayClient;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String status,
                       @RequestParam(required = false) String dateFrom,
                       @RequestParam(required = false) String dateTo,
                       Model model) {
        if (status != null && !status.isEmpty()) {
            model.addAttribute("orders", orderService.findByStatus(OrderStatus.valueOf(status)));
        } else {
            model.addAttribute("orders", orderService.findAll());
        }
        model.addAttribute("statuses", OrderStatus.values());
        return "admin/orders";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.findById(id).orElseThrow());
        return "admin/order-detail";
    }

    @PostMapping("/{id}/update-price")
    public String updatePrice(@PathVariable Long id, @RequestParam double shippingFee,
                              @RequestParam(required = false) Double newTotal,
                              RedirectAttributes ra) {
        Order order = orderService.findById(id).orElseThrow();
        order.setShippingFee(shippingFee);
        if (newTotal != null) order.setTotal(newTotal);
        orderService.save(order);
        ra.addFlashAttribute("success", "订单金额已更新");
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/{id}/ship")
    public String ship(@PathVariable Long id, @RequestParam String company,
                       @RequestParam String trackingNo, RedirectAttributes ra) {
        Order order = orderService.findById(id).orElseThrow();
        orderService.ship(order, company, trackingNo);
        ra.addFlashAttribute("success", "订单已标记为发货");
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes ra) {
        Order order = orderService.findById(id).orElseThrow();
        orderService.cancel(order);
        ra.addFlashAttribute("success", "订单已取消");
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/{id}/refund")
    public String refund(@PathVariable Long id, RedirectAttributes ra) {
        Order order = orderService.findById(id).orElseThrow();
        try {
            if (order.getTradeNo() == null || order.getTradeNo().isBlank()) throw new IllegalStateException("订单没有支付交易号，无法调用支付平台退款");
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            request.setBizContent("{\"out_trade_no\":\"" + order.getOrderNo() + "\",\"trade_no\":\"" + order.getTradeNo() + "\",\"refund_amount\":\"" + String.format(java.util.Locale.ROOT, "%.2f", order.getTotal()) + "\",\"refund_reason\":\"Nuobo order refund\"}");
            var response = alipayClient.execute(request);
            if (!response.isSuccess()) throw new IllegalStateException("支付平台退款失败：" + response.getSubMsg());
            orderService.refund(order);
            ra.addFlashAttribute("success", "支付平台已接受退款，订单状态已更新");
        } catch (AlipayApiException | IllegalStateException error) {
            ra.addFlashAttribute("error", error.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/{id}/email")
    public String sendEmail(@PathVariable Long id, @RequestParam String subject,
                            @RequestParam String message, RedirectAttributes ra) {
        Order order = orderService.findById(id).orElseThrow();
        emailService.sendOrderConfirmation(order.getUser().getEmail(), order.getOrderNo(), order.getTotal());
        ra.addFlashAttribute("success", "邮件已发送");
        return "redirect:/admin/orders/" + id;
    }
}
