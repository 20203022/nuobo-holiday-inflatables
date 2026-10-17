import com.noble.shop.controller.*;
import com.noble.shop.model.*;
import com.noble.shop.repository.UserRepository;
import com.noble.shop.service.*;
import com.noble.shop.service.OrderService.CartItem;
import java.lang.reflect.Proxy;
import java.util.*;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

public class FinalAuditCheck {
    static int checks;
    static void check(boolean valid, String name) {
        if (!valid) throw new AssertionError(name);
        checks++;
        System.out.println("PASS " + name);
    }
    static void notFound(Runnable action, String name) {
        try { action.run(); throw new AssertionError(name); }
        catch (ResponseStatusException error) { check(error.getStatusCode().value() == 404, name); }
    }
    static class Products extends ProductService {
        Product product = new Product();
        Products() {
            super(null);
            product.setId(1L); product.setPublished(true); product.setStock(Integer.MAX_VALUE);
        }
        public Optional<Product> findByIdWithImages(Long id) { return id == 1 ? Optional.of(product) : Optional.empty(); }
        public Optional<Product> findById(Long id) { return findByIdWithImages(id); }
    }
    static class Users extends UserService {
        User user = new User();
        int registrations, changes;
        String email;
        Users() { super(null, null); user.setId(1L); user.setEmail("buyer@example.test"); }
        public Optional<User> findByEmail(String value) { email = value; return Optional.empty(); }
        public User register(String name, String value, String phone, String password) {
            registrations++; user.setEmail(value); return user;
        }
        public void changePassword(Long id, String value) { changes++; }
    }
    static class Tokens extends AuthService {
        boolean valid;
        int uses;
        Tokens() { super(null, null, null); }
        public boolean validateResetToken(String token) { return valid; }
        public String getEmailByResetToken(String token) { return "buyer@example.test"; }
        public void markResetTokenUsed(String token) { uses++; }
    }
    static class Emails extends EmailService {
        Emails() { super(null); }
        public void sendWelcome(String email, String name) {}
    }
    static class Orders extends OrderService {
        Order order;
        Orders() { super(null, null, null, null, null); }
        public Optional<Order> findByOrderNo(String number) { return Optional.ofNullable(order); }
    }
    public static void main(String[] args) {
        var products = new Products();
        var cart = new CartController(products);
        var session = new MockHttpSession();
        cart.add(1L, 1, session);
        cart.add(1L, Integer.MAX_VALUE, session);
        var items = (List<CartItem>)session.getAttribute("cart");
        check(items.get(0).getQuantity() == Integer.MAX_VALUE, "large cart add does not overflow");
        cart.update(1L, 0, session);
        check(items.isEmpty(), "zero quantity removes cart item");
        notFound(() -> cart.add(99L, 1, session), "unknown cart product returns 404");
        products.product.setStock(5);
        cart.add(1L, 10, session);
        check(items.get(0).getQuantity() == 5, "cart clamps to stock");
        var savedQuantity = cart.updateAjax(1L, 3, session);
        check(savedQuantity.get("quantity").equals(3) && savedQuantity.get("stock").equals(5), "AJAX quantity response reflects stored cart and stock");
        products.product.setStock(0);
        cart.view(new ExtendedModelMap(), session);
        check(items.isEmpty(), "sold out cart product removed");

        var catalog = new ProductController(products);
        notFound(() -> catalog.list("missing", 0, null, null, null, null, new ExtendedModelMap()), "unknown category returns 404");
        products.product.setPublished(false);
        notFound(() -> catalog.detail(1L, new ExtendedModelMap()), "unpublished detail returns 404");

        var users = new Users();
        var tokens = new Tokens();
        var auth = new AuthController(users, tokens, new BCryptPasswordEncoder(), new Emails());
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        request.getSession();
        String oldSession = request.getSession().getId();
        auth.doRegister(" Buyer ", " BUYER@EXAMPLE.TEST ", "BuyerPassword42", request, response, new RedirectAttributesModelMap());
        check(users.email.equals("buyer@example.test"), "registration normalizes email before lookup");
        check(!request.getSession().getId().equals(oldSession), "registration rotates session identifier");
        SecurityContextHolder.clearContext();
        var nextRequest = new MockHttpServletRequest();
        nextRequest.setSession((MockHttpSession)request.getSession());
        var restored = new HttpSessionSecurityContextRepository().loadDeferredContext(nextRequest).get();
        check(restored.getAuthentication().getName().equals("buyer@example.test"), "registration persists login across requests");
        check(restored.getAuthentication().getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN")), "registration creates ordinary user");
        auth.doRegister("Buyer", "invalid", "weak", request, response, new RedirectAttributesModelMap());
        check(users.registrations == 1, "invalid registration does not create user");
        auth.doResetPassword("expired", "BuyerPassword42", new RedirectAttributesModelMap());
        check(users.changes == 0 && tokens.uses == 0, "expired reset POST cannot change password");
        tokens.valid = true;
        auth.doResetPassword("valid", "weak", new RedirectAttributesModelMap());
        check(users.changes == 0, "weak reset password rejected by server");

        var encoder = new BCryptPasswordEncoder();
        final User[] saved = {null};
        UserRepository repository = (UserRepository)Proxy.newProxyInstance(UserRepository.class.getClassLoader(), new Class[]{UserRepository.class}, (proxy, method, arguments) -> {
            if (method.getName().equals("save")) { saved[0] = (User)arguments[0]; return saved[0]; }
            throw new AssertionError("Unexpected repository operation");
        });
        new UserService(repository, encoder).register("Buyer", "buyer@example.test", "", "BuyerPassword42");
        check(encoder.matches("BuyerPassword42", saved[0].getPassword()), "registration stores usable hashed password");
        check(!saved[0].isEmailVerified(), "registration does not claim email verification");

        var orders = new Orders();
        var owner = new User(); owner.setEmail("owner@example.test");
        orders.order = new Order(); orders.order.setUser(owner); orders.order.setOrderNo("TEST");
        orders.order.setPaymentStatus(Order.PaymentStatus.PAID);
        var checkout = new CheckoutController(orders, null, null, users);
        var buyerAuth = new UsernamePasswordAuthenticationToken("buyer@example.test", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(buyerAuth);
        session.setAttribute("cart", new ArrayList<>());
        notFound(() -> checkout.success("TEST", null, null, null, session, new ExtendedModelMap()), "foreign order confirmation returns 404");
        check(session.getAttribute("cart") != null, "foreign order cannot clear buyer cart");
        owner.setEmail("buyer@example.test");
        check(checkout.success("TEST", null, null, null, session, new ExtendedModelMap()).equals("order-success"), "owner can view order confirmation");
        check(session.getAttribute("cart") == null, "paid own order clears cart");

        User profile = new User(); profile.setId(1L); profile.setEmailVerified(false); profile.setPassword(encoder.encode("OldPassword42"));
        Users profileUsers = new Users() { public Optional<User> findByEmail(String email) { return Optional.of(profile); } };
        var account = new AccountController(profileUsers, orders, encoder);
        account.updatePassword(buyerAuth, "wrong", "NewPassword42", new RedirectAttributesModelMap());
        check(profileUsers.changes == 0, "unverified email does not bypass current password");
        account.updatePassword(buyerAuth, "OldPassword42", "NewPassword42", new RedirectAttributesModelMap());
        check(profileUsers.changes == 1, "valid account password change allowed");
        SecurityContextHolder.clearContext();
        System.out.println("Isolated regression checks: " + checks + " passed; no database or mail connection.");
    }
}
