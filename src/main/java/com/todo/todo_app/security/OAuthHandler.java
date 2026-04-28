// OAuth2 Handler

package com.todo.todo_app.security;

import com.todo.todo_app.model.User;
import com.todo.todo_app.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuthHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    // Configurable via env
    @Value("${app.oauth.success-redirect-url}")
    private String successRedirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauthUser = token.getPrincipal();

        // Normalise the provider name
        String provider = token.getAuthorizedClientRegistrationId().toUpperCase();

        Object subObj = oauthUser.getAttribute("sub");
        Object idObj = oauthUser.getAttribute("id");

        if (subObj == null && idObj == null) {
            throw new RuntimeException("Cannot determine provider ID for OAuth user");
        }
        String providerId = subObj != null ? subObj.toString() : idObj.toString();

        String name = oauthUser.getAttribute("name");
        String email = oauthUser.getAttribute("email");

        if (email == null) {
            email = provider.toLowerCase() + "_" + providerId + "@noemail.local";
        }

        User user = userService.findOrCreateOAuthUser(provider, providerId, name, email);
        String jwt = jwtUtil.generateToken(user.getUserId());

        response.sendRedirect(successRedirectUrl
                + "#token=" + jwt
                + "&userId=" + user.getUserId()
                + "&provider=" + URLEncoder.encode(provider, StandardCharsets.UTF_8)
                + "&name=" + URLEncoder.encode(user.getName() == null ? "" : user.getName(), StandardCharsets.UTF_8)
                + "&email="
                + URLEncoder.encode(user.getEmail() == null ? "" : user.getEmail(), StandardCharsets.UTF_8));
    }
}
