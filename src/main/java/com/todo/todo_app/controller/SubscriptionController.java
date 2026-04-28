// Subscription Controller

package com.todo.todo_app.controller;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.todo.todo_app.model.User;
import com.todo.todo_app.service.UserService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import jakarta.annotation.PostConstruct;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;

@RestController
@RequestMapping("/api/subscription")
public class SubscriptionController {

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    private RazorpayClient razorpayClient;

    @PostConstruct
    public void init() throws RazorpayException {
        this.razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
    }

    private final UserService userService;

    public SubscriptionController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/create-order")
    public Map<String, Object> createOrder(@RequestParam(defaultValue = "monthly") String planType, Authentication auth) throws RazorpayException {
        Long userId = (Long) auth.getPrincipal();
        User user = userService.findById(userId);
        if (user.isPremium() && user.getSubscriptionExpiry() != null
                && LocalDateTime.now().isBefore(user.getSubscriptionExpiry())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You already have an active subscription");
        }
        int amount = 9900;
        if ("half-yearly".equals(planType)) amount = 29900;
        if ("yearly".equals(planType)) amount = 69900;

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amount);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "receipt_user_" + userId);

        Order order = razorpayClient.orders.create(orderRequest);

        return Map.of(
                "orderId", order.get("id"),
                "amount", amount,
                "currency", "INR",
                "keyId", razorpayKeyId);
    }

    @PostMapping("/verify-payment")
    public Map<String, Object> verifyPayment(@RequestBody Map<String, String> body, @RequestParam(defaultValue = "monthly") String planType, Authentication auth) {
        String orderId = body.get("razorpay_order_id");
        String paymentId = body.get("razorpay_payment_id");
        String signature = body.get("razorpay_signature");

        // Verify SHA256 signature
        String payload = orderId + "|" + paymentId;
        String expected = hmacSha256(payload, razorpayKeySecret);

        if (!expected.equals(signature)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment verification failed");
        }

        // Mark user as premium for selected plan
        Long userId = (Long) auth.getPrincipal();
        User user = userService.findById(userId);

        int daysToAdd = 30;
        if ("half-yearly".equals(planType)) daysToAdd = 180;
        if ("yearly".equals(planType)) daysToAdd = 365;

        user.setPremium(true);
        user.setSubscriptionExpiry(LocalDateTime.now().plusDays(daysToAdd));
        userService.save(user);

        return Map.of(
                "success", true,
                "isPremium", true,
                "subscriptionExpiry", user.getSubscriptionExpiry().toString());
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus(Authentication auth) {
        System.out.println(" SUBSCRIPTION STATUS ENDPOINT REACHED ");
        System.out.println("Auth object: " + auth);
        if (auth == null) {
            System.out.println("Auth is NULL in controller!");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        Long userId = (Long) auth.getPrincipal();
        User user = userService.findById(userId);

        boolean isPremium = user.isPremium();
        // Auto expire dynamically for GET calls
        if (isPremium && user.getSubscriptionExpiry() != null
                && LocalDateTime.now().isAfter(user.getSubscriptionExpiry())) {
            isPremium = false;
            user.setPremium(false);
            userService.save(user);
        }

        return Map.of(
                "isPremium", isPremium,
                "subscriptionExpiry", user.getSubscriptionExpiry() != null
                        ? user.getSubscriptionExpiry().toString()
                        : "");
    }

    private String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC", e);
        }
    }
}
