// Authentication Controller

package com.todo.todo_app.controller;

import com.todo.todo_app.model.User;
import com.todo.todo_app.security.JwtUtil;
import com.todo.todo_app.service.MfaService;
import com.todo.todo_app.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private MfaService mfaService;

    // Account creation
    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody User user) {
        User saved = userService.registerUser(user);
        String token = jwtUtil.generateToken(saved.getUserId());
        return new AuthResponse(saved.getUserId(), saved.getName(), saved.getEmail(), token);
    }

    // Account login
    @PostMapping("/login")
    public AuthResponse login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");
        User user = userService.loginUser(email, password);

        // MFA
        if (user.isMfaEnabled()) {
            String tempToken = jwtUtil.generateTempToken(user.getUserId());
            return new AuthResponse(true, tempToken);
        }

        // For normal user login
        String token = jwtUtil.generateToken(user.getUserId());
        return new AuthResponse(user.getUserId(), user.getName(), user.getEmail(), token);
    }

    // POST MFA for verification
    @PostMapping("/mfa-login")
    public AuthResponse mfaLogin(@RequestBody Map<String, Object> body) {
        String tempToken = (String) body.get("tempToken");
        Object rawCode = body.get("code");
        if (rawCode == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "code is required");
        int code = ((Number) rawCode).intValue();

        Long userId = jwtUtil.extractUserId(tempToken);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired temp token");
        }
        if (!jwtUtil.isMfaPending(tempToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid temp token");
        }

        User user = userService.findById(userId);

        boolean valid = mfaService.verifyCode(user.getMfaSecret(), code);
        if (!valid) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid MFA code");
        }

        String token = jwtUtil.generateToken(user.getUserId());
        return new AuthResponse(user.getUserId(), user.getName(), user.getEmail(), token);
    }
}