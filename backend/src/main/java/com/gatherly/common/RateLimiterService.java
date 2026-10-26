package com.gatherly.common;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {

    private final int maxRequestsPerMinute;
    private final Map<String, Deque<Instant>> requestLog = new ConcurrentHashMap<>();

    public RateLimiterService(
            @Value("${ai.rate-limit-per-minute:15}") int maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    public synchronized void checkLimit(String key) {
        checkLimit(
                key,
                maxRequestsPerMinute,
                "Too many AI planning requests. Limit is " + maxRequestsPerMinute + " requests per minute. Please wait a moment before trying again."
        );
    }

    public synchronized void checkLimit(String key, int maxRequests, String message) {
        Instant now = Instant.now();
        Instant oneMinuteAgo = now.minusSeconds(60);

        Deque<Instant> timestamps = requestLog.computeIfAbsent(key, k -> new ArrayDeque<>());

        // Evict timestamps older than 60 seconds
        while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(oneMinuteAgo)) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= maxRequests) {
            throw new RateLimitExceededException(message);
        }

        timestamps.addLast(now);
    }

    public void clear(String key) {
        requestLog.remove(key);
    }
}
