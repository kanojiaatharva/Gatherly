package com.gatherly.common;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping({"/health", "/api/health"})
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "gatherly-api");
    }
}
