package com.stockmonitor.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Filter to restrict SERVICE role access to specific endpoints only.
 *
 * Whitelist for SERVICE role:
 * - POST /api/runs (only if run_type=SCHEDULED)
 * - GET /api/runs/*
 * - GET /api/data-sources/*
 *
 * SERVICE role is blocked from:
 * - All PUT operations
 * - All DELETE operations
 * - Manual run triggers
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceRoleAccessFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    private static final Set<String> ALLOWED_SERVICE_PATHS = Set.of(
            "/api/runs",
            "/api/data-sources"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/ws");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Only apply restrictions if user has SERVICE role
        if (auth != null && hasServiceRole(auth)) {
            String method = request.getMethod();
            String path = request.getRequestURI();

            // Block all PUT and DELETE operations
            if ("PUT".equals(method) || "DELETE".equals(method)) {
                sendForbiddenResponse(response,
                        "SERVICE role cannot perform " + method + " operations. Only scheduled read operations are allowed.");
                return;
            }

            // For POST requests, apply stricter rules
            if ("POST".equals(method)) {
                if (path.startsWith("/api/runs")) {
                    // Allow only if run_type=SCHEDULED parameter is present
                    String runType = request.getParameter("run_type");
                    if (!"SCHEDULED".equals(runType)) {
                        sendForbiddenResponse(response,
                                "SERVICE role can only trigger SCHEDULED runs, not manual runs. Add parameter run_type=SCHEDULED");
                        return;
                    }
                } else {
                    // Block all other POST requests
                    sendForbiddenResponse(response,
                            "SERVICE role can only POST to /api/runs with run_type=SCHEDULED");
                    return;
                }
            }

            // For GET requests, verify path is in whitelist
            if ("GET".equals(method)) {
                boolean pathAllowed = ALLOWED_SERVICE_PATHS.stream()
                        .anyMatch(path::startsWith);

                if (!pathAllowed) {
                    sendForbiddenResponse(response,
                            "SERVICE role can only access: " + String.join(", ", ALLOWED_SERVICE_PATHS));
                    return;
                }
            }

            log.debug("SERVICE role access granted: {} {}", method, path);
        }

        filterChain.doFilter(request, response);
    }

    private boolean hasServiceRole(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SERVICE"));
    }

    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");

        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Forbidden");
        errorResponse.put("message", message);
        errorResponse.put("status", String.valueOf(HttpStatus.FORBIDDEN.value()));

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        log.warn("SERVICE role access denied: {}", message);
    }
}
