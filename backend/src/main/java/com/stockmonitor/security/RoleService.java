package com.stockmonitor.security;

import com.stockmonitor.model.User;
import com.stockmonitor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

/**
 * Service for role-based access control (RBAC) enforcement.
 *
 * Defines three roles per FR-061:
 * - ROLE_OWNER: Full access to portfolio (read, write, delete)
 * - ROLE_VIEWER: Read-only access to portfolio
 * - ROLE_SERVICE: Scheduled jobs only (limited write access)
 *
 * Used with @PreAuthorize annotations on controller methods.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {

    private final UserRepository userRepository;

    /**
     * Check if current user has OWNER role for the specified portfolio.
     *
     * @param portfolioId Portfolio ID to check ownership
     * @return true if current user is owner
     */
    public boolean isPortfolioOwner(UUID portfolioId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            log.warn("No authenticated user found for portfolio ownership check");
            return false;
        }

        UUID userId = UUID.fromString(auth.getName());
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            log.warn("User not found: {}", userId);
            return false;
        }

        // Check if user has OWNER role
        if (user.getRole() == null || user.getRole() != User.UserRole.OWNER) {
            log.warn("User {} does not have OWNER role", userId);
            return false;
        }

        // TODO: Verify portfolio actually belongs to this user
        // For now, OWNER role grants access to all portfolios
        return true;
    }

    /**
     * Check if current user has at least VIEWER role.
     * VIEWER can read portfolio data but cannot modify.
     *
     * @return true if user has VIEWER or OWNER role
     */
    public boolean hasViewerRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        UUID userId = UUID.fromString(auth.getName());
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            return false;
        }

        User.UserRole userRole = user.getRole();
        return userRole != null && (userRole == User.UserRole.VIEWER || userRole == User.UserRole.OWNER);
    }

    /**
     * Check if current user has SERVICE role.
     * SERVICE role is for scheduled jobs and background processes.
     *
     * @return true if user has SERVICE role
     */
    public boolean hasServiceRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SERVICE"));
    }

    /**
     * Check if current user has OWNER role.
     * OWNER has full access to create, modify, and delete.
     *
     * @return true if user has OWNER role
     */
    public boolean hasOwnerRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        UUID userId = UUID.fromString(auth.getName());
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            return false;
        }

        return user.getRole() != null && user.getRole() == User.UserRole.OWNER;
    }
}
