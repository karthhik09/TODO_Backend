// User Controller

package com.todo.todo_app.controller;

import com.todo.todo_app.model.User;
import com.todo.todo_app.service.UserService;
import com.todo.todo_app.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    // PUT for users
    @PutMapping("/{id}")
    public AuthResponse updateUser(@PathVariable Long id, @RequestBody User userDetails, Authentication auth) {
        Long authenticatedUserId = (Long) auth.getPrincipal();

        // Ownership check
        if (!authenticatedUserId.equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        User saved = userService.updateUser(id, userDetails);
        String token = jwtUtil.generateToken(saved.getUserId());
        // Return AuthResponse DTO
        return new AuthResponse(saved.getUserId(), saved.getName(), saved.getEmail(), token);
    }
}