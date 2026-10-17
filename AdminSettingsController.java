package com.noble.shop.controller.admin;

import com.noble.shop.service.ContactService;
import com.noble.shop.service.PageContentService;
import com.noble.shop.service.SettingsService;
import com.noble.shop.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/admin")
public class AdminSettingsController {

    private final ContactService contactService;
    private final UserService userService;
    private final SettingsService settingsService;
    private final PageContentService pageContentService;

    public AdminSettingsController(ContactService contactService, UserService userService,
                                   SettingsService settingsService, PageContentService pageContentService) {
        this.contactService = contactService;
        this.userService = userService;
        this.settingsService = settingsService;
        this.pageContentService = pageContentService;
    }

    @GetMapping("/messages")
    public String messages(@RequestParam(required = false) String status,
                           @RequestParam(required = false) String q, Model model) {
        model.addAttribute("messages", contactService.findByStatusAndQuery(status, q));
        model.addAttribute("selectedStatus", status == null ? "" : status);
        model.addAttribute("selectedQuery", q == null ? "" : q);
        model.addAttribute("newLeadCount", contactService.countNew());
        model.addAttribute("overdueLeadCount", contactService.countOverdueNew());
        return "admin/messages";
    }

    @GetMapping("/messages/export")
    public ResponseEntity<byte[]> exportMessages(@RequestParam(required = false) String status,
                                                  @RequestParam(required = false) String q) {
        StringBuilder csv = new StringBuilder("ID,Status,Name,Email,Company,Country,Product,Quantity,Created At\n");
        for (var message : contactService.findByStatusAndQuery(status, q)) {
            csv.append(csvValue(message.getId())).append(',').append(csvValue(message.getStatus())).append(',')
                .append(csvValue(message.getName())).append(',').append(csvValue(message.getEmail())).append(',')
                .append(csvValue(message.getCompany())).append(',').append(csvValue(message.getCountry())).append(',')
                .append(csvValue(message.getProductModel())).append(',').append(csvValue(message.getInquiryQuantity())).append(',')
                .append(csvValue(message.getCreatedAt())).append('\n');
        }
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=nuobo-inquiries.csv")
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8)).body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String csvValue(Object value) {
        String text = value == null ? "" : String.valueOf(value).replace("\"", "\"\"");
        return '"' + text + '"';
    }

    @PostMapping("/messages/delete/{id}")
    public String deleteMessage(@PathVariable Long id, RedirectAttributes ra) {
        contactService.delete(id);
        ra.addFlashAttribute("success", "留言已删除");
        return "redirect:/admin/messages";
    }

    @PostMapping("/messages/read/{id}")
    public String markMessageRead(@PathVariable Long id, RedirectAttributes ra) {
        contactService.markRead(id);
        ra.addFlashAttribute("success", "留言已标记为已读");
        return "redirect:/admin/messages";
    }

    @PostMapping("/messages/status/{id}")
    public String updateMessageStatus(@PathVariable Long id,
                                      @RequestParam String status,
                                      @RequestParam(required = false) String internalNote,
                                      RedirectAttributes ra) {
        contactService.updateStatus(id, status, internalNote);
        ra.addFlashAttribute("success", "线索状态已更新");
        return "redirect:/admin/messages";
    }

    @GetMapping("/users")
    public String users(Model model) {
        var users = userService.findAll();
        model.addAttribute("users", users);
        model.addAttribute("adminCount", users.stream().filter(u -> u.isAdmin()).count());
        model.addAttribute("activeCount", users.stream().filter(u -> u.isActive()).count());
        model.addAttribute("verifiedCount", users.stream().filter(u -> u.isEmailVerified()).count());
        return "admin/users";
    }

    @PostMapping("/users/{id}/active")
    public String setUserActive(@PathVariable Long id, @RequestParam boolean active, RedirectAttributes ra) {
        userService.setActive(id, active);
        ra.addFlashAttribute("success", active ? "账号已启用" : "账号已停用");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/password")
    public String resetUserPassword(@PathVariable Long id, @RequestParam String password, RedirectAttributes ra) {
        userService.changePassword(id, password);
        ra.addFlashAttribute("success", "密码已重置");
        return "redirect:/admin/users";
    }

    @GetMapping("/shipping")
    public String shipping(Model model) {
        model.addAttribute("shippingFee", settingsService.get("shipping_fee", "9.99"));
        model.addAttribute("freeShippingThreshold", settingsService.get("free_shipping_threshold", "50"));
        return "admin/shipping";
    }

    @PostMapping("/shipping")
    public String saveShipping(@RequestParam double shippingFee,
                               @RequestParam double freeShippingThreshold,
                               RedirectAttributes ra) {
        settingsService.set("shipping_fee", String.valueOf(shippingFee));
        settingsService.set("free_shipping_threshold", String.valueOf(freeShippingThreshold));
        ra.addFlashAttribute("success", "运费设置已保存");
        return "redirect:/admin/shipping";
    }

    @GetMapping("/pages")
    public String pages(Model model) {
        model.addAttribute("about", pageContentService.getContent("about"));
        model.addAttribute("faq", pageContentService.getContent("faq"));
        model.addAttribute("privacy", pageContentService.getContent("privacy"));
        model.addAttribute("returnPolicy", pageContentService.getContent("return-policy"));
        return "admin/pages";
    }

    @PostMapping("/pages")
    public String savePage(@RequestParam String pageKey,
                           @RequestParam String contentCn,
                           @RequestParam String contentEn,
                           RedirectAttributes ra) {
        pageContentService.save(pageKey, contentCn, contentEn);
        ra.addFlashAttribute("success", "页面内容已保存");
        return "redirect:/admin/pages";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("siteName", settingsService.get("site_name", "nuobo"));
        model.addAttribute("siteCopyright", settingsService.get("site_copyright", "2026 nuobo. All Rights Reserved."));
        return "admin/settings";
    }

    @PostMapping("/settings")
    public String saveSettings(@RequestParam String siteName,
                               @RequestParam String siteCopyright,
                               RedirectAttributes ra) {
        settingsService.set("site_name", siteName);
        settingsService.set("site_copyright", siteCopyright);
        ra.addFlashAttribute("success", "网站设置已保存");
        return "redirect:/admin/settings";
    }
}
