// MFA Controller

package com.todo.todo_app.controller;

import com.todo.todo_app.model.User;
import com.todo.todo_app.service.UserService;
import com.todo.todo_app.service.MfaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/mfa")
public class MfaController {

    @Autowired
    private MfaService mfaService;

    @Autowired
    private UserService userService;

    // POST for MFA setup
    @PostMapping("/setup")
    public Map<String, String> setup(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User user = userService.findById(userId);

        String secret = mfaService.generateSecret();
        user.setMfaSecret(secret);
        userService.save(user);

        String qrUri = mfaService.getQrUri(user.getEmail(), secret);
        return Map.of("secret", secret, "qrUri", qrUri);
    }

    // POST for verify setup
    @PostMapping("/verify-setup")
    public Map<String, Boolean> verifySetup(@RequestBody Map<String, Integer> body, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User user = userService.findById(userId);

        if (user.getMfaSecret() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA setup not initiated");
        }

        // Extract the code
        Integer code = body.get("code");
        if (code == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A 6 digit code is required");
        }
        boolean valid = mfaService.verifyCode(user.getMfaSecret(), code);
        if (!valid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid code");
        }

        user.setMfaEnabled(true);
        userService.save(user);
        return Map.of("success", true);
    }

    // POST for MFA disable
    @PostMapping("/disable")
    public Map<String, Boolean> disable(@RequestBody Map<String, Integer> body, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User user = userService.findById(userId);

        if (!user.isMfaEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA is not enabled");
        }

        Integer code = body.get("code");
        if (code == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A 6-digit code is required");
        }
        boolean valid = mfaService.verifyCode(user.getMfaSecret(), code);
        if (!valid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid code");
        }

        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userService.save(user);
        return Map.of("success", true);
    }
}