package com.vitto.lending.controller;

import com.vitto.lending.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private MongoTemplate mongoTemplate;

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> checkHealth() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");

        // Check Postgres
        if (jdbcTemplate != null) {
            try {
                jdbcTemplate.execute("SELECT 1");
                health.put("postgres", "UP");
            } catch (Exception e) {
                health.put("postgres", "DOWN");
                health.put("status", "DOWN");
            }
        } else {
            health.put("postgres", "DOWN");
            health.put("status", "DOWN");
        }

        // Check Mongo
        if (mongoTemplate != null) {
            try {
                Document ping = mongoTemplate.getDb().runCommand(new Document("ping", 1));
                if (ping.containsKey("ok")) {
                    health.put("mongo", "UP");
                } else {
                    health.put("mongo", "DOWN");
                    health.put("status", "DOWN");
                }
            } catch (Exception e) {
                health.put("mongo", "DOWN");
                health.put("status", "DOWN");
            }
        } else {
            health.put("mongo", "DOWN");
            health.put("status", "DOWN");
        }

        return ApiResponse.success(health);
    }
}
