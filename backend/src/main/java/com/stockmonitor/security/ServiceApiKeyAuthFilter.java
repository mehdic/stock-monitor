package com.stockmonitor.security;

import com.stockmonitor.model.ServiceApiKey;
import com.stockmonitor.repository.ServiceApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

/**
 * Filter to authenticate service API keys from X-Service-Api-Key header.
 * Validates API key, checks expiration, and sets authentication with SERVICE role.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceApiKeyAuthFilter extends OncePerRequestFilter {

    private final ServiceApiKeyRepository serviceApiKeyRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String API_KEY_HEADER = "X-Service-Api-Key";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/ws");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticateServiceApiKey(apiKey, request);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateServiceApiKey(String apiKey, HttpServletRequest request) {
        try {
            // Find all active keys and check if provided key matches any hash
            Optional<ServiceApiKey> matchingKey = serviceApiKeyRepository.findByIsActiveTrue()
                    .stream()
                    .filter(key -> passwordEncoder.matches(apiKey, key.getKeyHash()))
                    .findFirst();

            if (matchingKey.isPresent()) {
                ServiceApiKey serviceKey = matchingKey.get();

                // Check if key is valid (not expired)
                if (!serviceKey.isValid()) {
                    log.warn("Service API key is invalid (expired or inactive): {}", serviceKey.getName());
                    return;
                }

                // Update last used timestamp
                serviceKey.setLastUsedAt(LocalDateTime.now());
                serviceApiKeyRepository.save(serviceKey);

                // Create authentication with SERVICE role
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                "SERVICE_" + serviceKey.getName(),
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_SERVICE"))
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("Service authenticated successfully: {}", serviceKey.getName());
            } else {
                log.warn("Invalid service API key provided");
            }
        } catch (Exception e) {
            log.error("Error authenticating service API key", e);
        }
    }
}
