package com.vitto.lending.config;

import com.vitto.lending.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RateLimiterInterceptor implements HandlerInterceptor {

    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    
    // Key: Client IP (or some identifier), Value: Bucket
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIp(request);
        
        TokenBucket bucket = buckets.computeIfAbsent(clientIp, k -> new TokenBucket(MAX_REQUESTS_PER_MINUTE));
        
        if (!bucket.tryConsume()) {
            throw new RateLimitExceededException("Too many decision requests. Please try again later.");
        }
        
        return true;
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
    
    private static class TokenBucket {
        private final int capacity;
        private final AtomicInteger tokens;
        private final AtomicLong lastRefillTimestamp;
        
        public TokenBucket(int capacity) {
            this.capacity = capacity;
            this.tokens = new AtomicInteger(capacity);
            this.lastRefillTimestamp = new AtomicLong(System.currentTimeMillis());
        }
        
        public synchronized boolean tryConsume() {
            refill();
            if (tokens.get() > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
        }
        
        private void refill() {
            long now = System.currentTimeMillis();
            long lastRefill = lastRefillTimestamp.get();
            // Refill every 1 minute (60000 ms)
            if (now - lastRefill > 60000) {
                tokens.set(capacity);
                lastRefillTimestamp.set(now);
            }
        }
    }
}
