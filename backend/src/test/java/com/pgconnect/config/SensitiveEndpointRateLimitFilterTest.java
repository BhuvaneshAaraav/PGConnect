package com.pgconnect.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensitiveEndpointRateLimitFilterTest {
    @Test
    void limitsRepeatedLoginRequestsFromOneClient() throws Exception {
        SensitiveEndpointRateLimitFilter filter = new SensitiveEndpointRateLimitFilter();
        AtomicInteger forwarded = new AtomicInteger();
        FilterChain chain = (request, response) -> forwarded.incrementAndGet();

        for (int attempt = 0; attempt < 10; attempt++) {
            MockHttpServletRequest request = request("/api/auth/login");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, chain);
            assertEquals(200, response.getStatus());
        }

        MockHttpServletResponse limited = new MockHttpServletResponse();
        filter.doFilter(request("/api/auth/login"), limited, chain);

        assertEquals(429, limited.getStatus());
        assertTrue(Integer.parseInt(limited.getHeader("Retry-After")) > 0);
        assertTrue(limited.getContentAsString().contains("Too Many Requests"));
        assertEquals(10, forwarded.get());
    }

    private static MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr("203.0.113.5");
        return request;
    }
}
