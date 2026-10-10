package com.clearfolio.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.clearfolio.model.ResumeData;
import com.clearfolio.model.ResumePage;
import com.clearfolio.model.ResumeRecord;

@Service
public class ResumeHistoryService {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ResumeHistoryService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public ResumeRecord save(String filename, ResumeData data) {
        UUID id = UUID.randomUUID();
        Instant parsedAt = Instant.now();
        jdbcTemplate.update(
                "INSERT INTO resumes (id, filename, parsed_at, data_json) VALUES (?, ?, ?, ?)",
                id.toString(), filename, parsedAt.toString(), serialize(data));
        return new ResumeRecord(id, filename, parsedAt, data);
    }

    public ResumePage list(int page, int pageSize) {
        long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM resumes", Long.class);
        List<ResumeRecord> records = jdbcTemplate.query(
                "SELECT id, filename, parsed_at, data_json FROM resumes "
                        + "ORDER BY parsed_at DESC, id DESC LIMIT ? OFFSET ?",
                (row, rowNumber) -> toRecord(
                        row.getString("id"),
                        row.getString("filename"),
                        row.getString("parsed_at"),
                        row.getString("data_json")),
                pageSize,
                (long) (page - 1) * pageSize);
        return new ResumePage(records, total, page, pageSize);
    }

    public ResumeRecord get(UUID id) {
        List<ResumeRecord> records = jdbcTemplate.query(
                "SELECT id, filename, parsed_at, data_json FROM resumes WHERE id = ?",
                (row, rowNumber) -> toRecord(
                        row.getString("id"),
                        row.getString("filename"),
                        row.getString("parsed_at"),
                        row.getString("data_json")),
                id.toString());
        return records.isEmpty() ? null : records.getFirst();
    }

    public boolean delete(UUID id) {
        return jdbcTemplate.update("DELETE FROM resumes WHERE id = ?", id.toString()) > 0;
    }

    private ResumeRecord toRecord(String id, String filename, String parsedAt, String dataJson) {
        try {
            return new ResumeRecord(
                    UUID.fromString(id),
                    filename,
                    Instant.parse(parsedAt),
                    objectMapper.readValue(dataJson, ResumeData.class));
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new IllegalStateException("Stored resume history data is invalid.", exception);
        }
    }

    private String serialize(ResumeData data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize parsed resume data.", exception);
        }
    }
}
