package com.nuaa.recycle.controller;

import com.nuaa.recycle.common.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public R<Map<String, String>> health() {
        return R.ok(Map.of(
                "status", "UP",
                "time", LocalDateTime.now().toString(),
                "version", "1.0.0"
        ));
    }
}
