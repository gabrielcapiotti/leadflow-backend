package com.leadflow.backend.config;

import com.leadflow.backend.exception.SubscriptionInactiveException;
import com.leadflow.backend.security.VendorContext;
import com.leadflow.backend.service.vendor.SubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * Interceptor that validates subscription status before processing requests
 * to protected endpoints. Ensures that only tenants with active subscriptions
 * can access API endpoints.
 * 
 * If the subscription is not active, it throws SubscriptionInactiveException,
 * which is caught by BillingExceptionHandler and returns HTTP 402.
 */
@Slf4j
@Component
public class BillingValidationInterceptor implements HandlerInterceptor {

    private final SubscriptionService subscriptionService;
    private final VendorContext vendorContext;

    // Paths that are exempt from subscription validation
    private static final String[] PUBLIC_PATHS = {
        "/api/v1/auth",
        "/api/v1/health",
        "/api/v1/public",
        "/stripe/webhook",
        "/actuator",
        "/swagger-ui",
        "/v3/api-docs"
    };

    public BillingValidationInterceptor(
            SubscriptionService subscriptionService,
            VendorContext vendorContext) {
        this.subscriptionService = subscriptionService;
        this.vendorContext = vendorContext;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) throws Exception {

        String requestPath = request.getRequestURI();
        String method = request.getMethod();

        // Skip validation for public endpoints
        if (isPublicPath(requestPath)) {
            log.debug("Skipping billing validation for public path: {}", requestPath);
            return true;
        }

        // Only validate on actual request methods (skip OPTIONS)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        try {
            // Check if user is authenticated
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("Request to {} with no authentication", requestPath);
                return true; // Let Spring Security handle unauthenticated requests
            }

            // Get tenantId (vendorId) from VendorContext
            UUID tenantId = vendorContext.getCurrentVendorId();

            if (tenantId == null) {
                log.warn("Could not extract vendorId from VendorContext for path: {}", requestPath);
                throw new SubscriptionInactiveException(
                    "Vendor context not available",
                    "VENDOR_NOT_FOUND"
                );
            }

            // Validate subscription is active
            log.debug("Validating subscription for vendor: {} on path: {}", tenantId, requestPath);
            subscriptionService.validateActiveSubscription(tenantId);

            log.debug("Subscription validation passed for vendor: {} on path: {}", tenantId, requestPath);
            return true;

        } catch (SubscriptionInactiveException e) {
            log.warn("Subscription validation failed: {}", e.getMessage());
            throw e; // Let BillingExceptionHandler catch this
        } catch (Exception e) {
            log.error("Unexpected error during billing validation for path: {}", requestPath, e);
            throw new SubscriptionInactiveException(
                "Billing validation failed: " + e.getMessage(),
                "BILLING_VALIDATION_ERROR"
            );
        }
    }

    /**
     * Checks if the request path is a public endpoint that should skip billing validation
     */
    private boolean isPublicPath(String requestPath) {
        for (String publicPath : PUBLIC_PATHS) {
            if (requestPath.startsWith(publicPath)) {
                return true;
            }
        }
        return false;
    }
}
