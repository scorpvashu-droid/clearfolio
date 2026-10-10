package com.clearfolio.model;

import java.util.List;

public record Experience(
        String company,
        String role,
        String jobTitle,
        String duration,
        String startDate,
        String endDate,
        String description,
        List<String> skillsUsed
) {}
