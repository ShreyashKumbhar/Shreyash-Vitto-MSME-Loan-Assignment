package com.vitto.lending.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.bson.Document;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
public class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private MongoTemplate mongoTemplate;

    @Test
    public void testHealth_Success() throws Exception {
        // Mock Postgres SELECT 1 succeeding
        doNothing().when(jdbcTemplate).execute(any(String.class));

        // Mock MongoDB ping command returning ok
        com.mongodb.client.MongoDatabase mockDb = mock(com.mongodb.client.MongoDatabase.class);
        Document pingResult = new Document("ok", 1.0);
        when(mongoTemplate.getDb()).thenReturn(mockDb);
        when(mockDb.runCommand(any(Document.class))).thenReturn(pingResult);

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.postgres").value("UP"))
                .andExpect(jsonPath("$.data.mongo").value("UP"))
                .andExpect(jsonPath("$.error").isEmpty());
    }

    @Test
    public void testHealth_PostgresDown() throws Exception {
        // Mock Postgres throwing error
        doThrow(new RuntimeException("DB Connection Timeout")).when(jdbcTemplate).execute(any(String.class));

        // Mock MongoDB ping command returning ok
        com.mongodb.client.MongoDatabase mockDb = mock(com.mongodb.client.MongoDatabase.class);
        Document pingResult = new Document("ok", 1.0);
        when(mongoTemplate.getDb()).thenReturn(mockDb);
        when(mockDb.runCommand(any(Document.class))).thenReturn(pingResult);

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DOWN"))
                .andExpect(jsonPath("$.data.postgres").value("DOWN"))
                .andExpect(jsonPath("$.data.mongo").value("UP"));
    }
}
