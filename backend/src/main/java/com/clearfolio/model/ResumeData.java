package com.clearfolio.model;

import java.util.List;

public record ResumeData(
        String name,
        String email,
        String phone,
        String location,
        String summary,
        Double totalExperienceYears,
        List<String> skills,
        List<String> technicalSkills,
        List<String> softSkills,
        List<Education> education,
        List<Experience> experiences,
        List<String> projects,
        List<String> certifications,
        List<String> languages
) {}
