package com.clearfolio.model;

import java.util.List;

public record ResumePage(
        List<ResumeRecord> items,
        long total,
        int page,
        int pageSize
) {}
