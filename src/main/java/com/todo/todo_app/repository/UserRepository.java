// User Repository

package com.todo.todo_app.repository;

import com.todo.todo_app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // Find user by email (used by password-based login)
    Optional<User> findByEmail(String email);

    // Find OAuth user by provider name + provider's own user ID
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
}