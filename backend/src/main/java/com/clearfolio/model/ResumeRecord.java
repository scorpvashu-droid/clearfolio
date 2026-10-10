package com.clearfolio.model;

import java.time.Instant;
import java.util.UUID;

public record ResumeRecord(
        UUID id,
        String filename,
        Instant parsedAt,
        ResumeData data
) {}
