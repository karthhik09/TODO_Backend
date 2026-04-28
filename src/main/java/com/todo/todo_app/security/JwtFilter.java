// JWT Filter

package com.todo.todo_app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // OAuth2 endpoints pass through without JWT processing
        if (path.startsWith("/oauth2/") || path.startsWith("/login/oauth2/") || path.startsWith("/api/auth/")) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (!token.isBlank()) {
                Long userId = jwtUtil.extractUserId(token);
                boolean tokenValid = jwtUtil.isTokenValid(token);
                boolean mfaPending = jwtUtil.isMfaPending(token);

                System.out.println(" JWT DEBUG ");
                System.out.println("userId: " + userId);
                System.out.println("tokenValid: " + tokenValid);
                System.out.println("mfaPending: " + mfaPending);
                System.out.println("existingAuth: " + SecurityContextHolder.getContext().getAuthentication());

                if (userId != null && tokenValid && !mfaPending
                        && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userId, null, List.of());
                    
                    // Create a fresh context and set it
                    org.springframework.security.core.context.SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(auth);
                    SecurityContextHolder.setContext(context);
                    
                    System.out.println("Auth SET for userId: " + userId);
                    System.out.println("Context in holder after set: " + SecurityContextHolder.getContext().getAuthentication());
                } else {
                    System.out.println("Auth NOT set - reason: userId =" + userId
                        + " valid =" + tokenValid + " mfaPending =" + mfaPending);
                }
                System.out.println(" END JWT DEBUG ");
            }
        }

        try {
            chain.doFilter(request, response);
        } catch (Exception e) {
            System.out.println(" FILTER CHAIN ERROR ");
            e.printStackTrace();
            throw e;
        }
    }
}
