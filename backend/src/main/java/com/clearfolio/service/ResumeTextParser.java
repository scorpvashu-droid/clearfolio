package com.clearfolio.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.clearfolio.model.Education;
import com.clearfolio.model.Experience;
import com.clearfolio.model.ResumeData;

@Service
public class ResumeTextParser {
    private static final Pattern EMAIL = Pattern.compile(
            "\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE = Pattern.compile("(?<!\\w)(?:\\+?\\d[\\d().\\-\\s]{6,}\\d)(?!\\w)");
    private static final Pattern YEARS_EXPERIENCE = Pattern.compile(
            "\\b(\\d+(?:\\.\\d+)?)\\+?\\s+years?\\s+(?:of\\s+)?experience\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE = Pattern.compile(
            "\\b(?<start>(?:(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|"
                    + "Jul(?:y)?|Aug(?:ust)?|Sep(?:t(?:ember)?)?|Oct(?:ober)?|Nov(?:ember)?|"
                    + "Dec(?:ember)?\\.?)\\s+)?(?:19|20)\\d{2})\\s*(?:[-–—]|to)\\s*"
                    + "(?<end>(?:(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|"
                    + "Jul(?:y)?|Aug(?:ust)?|Sep(?:t(?:ember)?)?|Oct(?:ober)?|Nov(?:ember)?|"
                    + "Dec(?:ember)?\\.?)\\s+)?(?:19|20)\\d{2}|Present|Current|Now)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DEGREE = Pattern.compile(
            "\\b(Ph\\.?D\\.?|Doctor(?:ate)?|M\\.?S\\.?|M\\.?A\\.?|M\\.?B\\.?A\\.?|B\\.?S\\.?|"
                    + "B\\.?A\\.?|B\\.?E\\.?|B\\.?Tech|Bachelor(?:'s)?|Master(?:'s)?|"
                    + "Associate(?:'s)?|Diploma)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TITLE = Pattern.compile(
            "\\b(engineer|developer|manager|analyst|designer|consultant|intern|architect|"
                    + "administrator|specialist|director|scientist|lead|coordinator|officer|"
                    + "technician|researcher|product owner|accountant)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COMPANY_SUFFIX = Pattern.compile(
            "\\b(?:Inc\\.?|LLC|Ltd\\.?|Corporation|Corp\\.?|Technologies|University|Company|Group|Solutions|Systems)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final List<String> TECHNICAL_SKILLS = List.of(
            "Python", "Java", "JavaScript", "TypeScript", "C++", "C#", "Go", "Rust", "SQL", "HTML", "CSS",
            "React", "Angular", "Vue", "Node.js", "FastAPI", "Django", "Flask", "Spring", "AWS", "Azure", "GCP",
            "Docker", "Kubernetes", "Git", "Linux", "PostgreSQL", "MySQL", "SQLite", "MongoDB", "Redis",
            "TensorFlow", "PyTorch", "Pandas", "NumPy", "scikit-learn", "REST", "GraphQL", "CI/CD", "Jenkins",
            "Terraform", "Excel", "Power BI", "Tableau");
    private static final List<String> SOFT_SKILLS = List.of(
            "communication", "leadership", "teamwork", "problem solving", "problem-solving", "adaptability",
            "collaboration", "time management", "critical thinking", "creativity", "mentoring", "organization",
            "attention to detail", "conflict resolution", "customer service");
    private static final Map<String, String> SECTION_ALIASES = sectionAliases();

    public ResumeData parse(String resumeText) {
        if (resumeText == null || resumeText.isBlank()) {
            throw new ResumeParseException(422, "No readable text was found in the resume.");
        }

        ParsedSections parsed = splitSections(resumeText);
        String name = parsed.topLines().stream().limit(6)
                .filter(line -> !EMAIL.matcher(line).find() && !PHONE.matcher(line).find())
                .filter(line -> !Pattern.compile("\\b(resume|curriculum vitae|linkedin\\.com|github\\.com)\\b",
                        Pattern.CASE_INSENSITIVE).matcher(line).find())
                .filter(line -> !Pattern.compile("^\\s*(?:location|based in|address)\\s*[:\\-]",
                        Pattern.CASE_INSENSITIVE).matcher(line).find())
                .filter(line -> line.length() <= 70 && !line.matches(".*[@:/].*"))
                .findFirst().orElse(null);
        String email = firstMatch(EMAIL, resumeText);
        String phone = firstMatch(PHONE, resumeText);
        String location = findLocation(parsed.topLines());
        String summary = sectionText(parsed.sections().get("summary"));

        List<String> explicitSkills = items(parsed.sections().get("skills"));
        List<String> technical = unique(concat(explicitSkills, recognized(resumeText, TECHNICAL_SKILLS)));
        List<String> soft = recognized(resumeText, SOFT_SKILLS);
        List<String> skills = unique(concat(technical, soft));

        Matcher yearsMatcher = YEARS_EXPERIENCE.matcher(resumeText);
        Double years = yearsMatcher.find() ? Double.valueOf(yearsMatcher.group(1)) : null;

        return new ResumeData(
                name, email, phone, location, summary, years, skills, technical, soft,
                parseEducation(parsed.sections().get("education")),
                parseExperience(parsed.sections().get("experience")),
                items(parsed.sections().get("projects")),
                items(parsed.sections().get("certifications")),
                items(parsed.sections().get("languages")));
    }

    private ParsedSections splitSections(String text) {
        List<String> top = new ArrayList<>();
        Map<String, List<String>> sections = new LinkedHashMap<>();
        String current = null;
        for (String original : text.split("\\R")) {
            String line = cleanLine(original);
            String key = SECTION_ALIASES.get(line.replaceFirst("[:\\s]+$", "").replaceFirst("^\\d+[.)]\\s*", "")
                    .toLowerCase(Locale.ROOT));
            if (key != null) {
                current = key;
                sections.computeIfAbsent(key, ignored -> new ArrayList<>());
            } else if (current != null) {
                sections.get(current).add(line);
            } else if (!line.isBlank()) {
                top.add(line);
            }
        }
        return new ParsedSections(top, sections);
    }

    private String findLocation(List<String> lines) {
        for (String line : lines.stream().limit(8).toList()) {
            Matcher matcher = Pattern.compile("\\b(?:location|based in|address)\\s*[:\\-]\\s*(.+)$",
                    Pattern.CASE_INSENSITIVE).matcher(line);
            if (matcher.find()) {
                return matcher.group(1).replaceAll("^[ ,|]+|[ ,|]+$", "");
            }
        }
        for (String line : lines.stream().limit(5).toList()) {
            if (!EMAIL.matcher(line).find() && !PHONE.matcher(line).find()) {
                continue;
            }
            for (String part : line.split("\\s*[|•]\\s*")) {
                String candidate = PHONE.matcher(EMAIL.matcher(part).replaceAll("")).replaceAll("")
                        .replaceAll("^[ ,|]+|[ ,|]+$", "");
                if (candidate.contains(",") && !candidate.matches(".*\\d.*")) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private List<Experience> parseExperience(List<String> lines) {
        List<Experience> experiences = new ArrayList<>();
        for (List<String> chunk : chunks(lines)) {
            String raw = String.join(" ", chunk);
            Matcher date = DATE.matcher(raw);
            boolean hasDate = date.find();
            String startDate = hasDate ? date.group("start") : null;
            String endDate = hasDate ? date.group("end") : null;
            String duration = hasDate ? date.group() : null;

            String company = labelValue(raw, "company|employer");
            String title = labelValue(raw, "title|role|position");
            Set<String> metadata = new LinkedHashSet<>();
            for (String line : chunk) {
                String clean = DATE.matcher(line).replaceAll("").replaceAll("^[\\s\\-–—]+|[\\s\\-–—]+$", "");
                Matcher relationship = Pattern.compile("(?<title>.+?)\\s+(?:at|@)\\s+(?<company>.+)",
                        Pattern.CASE_INSENSITIVE).matcher(clean);
                if (relationship.find()) {
                    if (title == null) title = relationship.group("title").strip();
                    if (company == null) company = relationship.group("company").strip();
                    metadata.add(line);
                    continue;
                }
                if (title == null && TITLE.matcher(clean).find()) {
                    title = clean;
                    metadata.add(line);
                }
                if (company == null && COMPANY_SUFFIX.matcher(clean).find()) {
                    company = clean;
                    metadata.add(line);
                }
            }
            for (String line : chunk) {
                if (line.matches("(?i)^\\s*(?:company|employer|title|role|position)\\s*:.*")) {
                    metadata.add(line);
                }
            }
            String description = chunk.stream().filter(line -> !metadata.contains(line))
                    .filter(line -> !DATE.matcher(line).find()).collect(Collectors.joining(" "));
            if (description.isBlank()) description = null;

            experiences.add(new Experience(company, title, title, duration, startDate, endDate,
                    description, recognized(raw, TECHNICAL_SKILLS)));
        }
        return experiences;
    }

    private List<Education> parseEducation(List<String> lines) {
        List<Education> education = new ArrayList<>();
        for (List<String> chunk : chunks(lines)) {
            String raw = String.join(" ", chunk);
            Matcher degreeMatcher = DEGREE.matcher(raw);
            String degree = degreeMatcher.find() ? degreeMatcher.group() : null;
            String institution = chunk.stream()
                    .filter(line -> Pattern.compile("\\b(university|college|institute|school|academy)\\b",
                            Pattern.CASE_INSENSITIVE).matcher(line).find())
                    .findFirst().orElse(null);
            String field = null;
            if (degree != null) {
                Matcher fieldMatcher = Pattern.compile("\\b(?:in|of)\\s+([A-Z][A-Za-z &/-]+)")
                        .matcher(raw.substring(degreeMatcher.end()));
                if (fieldMatcher.find()) field = fieldMatcher.group(1).strip();
            }
            Matcher date = DATE.matcher(raw);
            String dates = date.find() ? date.group() : null;
            education.add(new Education(institution, degree, field, dates, raw.isBlank() ? null : raw));
        }
        return education;
    }

    private List<List<String>> chunks(List<String> lines) {
        if (lines == null) return List.of();
        List<List<String>> result = new ArrayList<>();
        List<String> current = new ArrayList<>();
        for (String line : lines) {
            if (line.isBlank()) {
                if (!current.isEmpty()) {
                    result.add(current);
                    current = new ArrayList<>();
                }
                continue;
            }
            boolean hasDate = current.stream().anyMatch(existing -> DATE.matcher(existing).find());
            boolean startsEntry = hasDate && line.length() < 100 && !line.matches(".*[.!?]$")
                    && (TITLE.matcher(line).find() || DEGREE.matcher(line).find());
            if (!current.isEmpty() && (startsEntry || DATE.matcher(line).find() && hasDate)) {
                result.add(current);
                current = new ArrayList<>();
            }
            current.add(line);
        }
        if (!current.isEmpty()) result.add(current);
        return result;
    }

    private String labelValue(String raw, String labels) {
        Matcher matcher = Pattern.compile("\\b(?:" + labels + ")\\s*:\\s*(.+)", Pattern.CASE_INSENSITIVE)
                .matcher(raw);
        return matcher.find() ? matcher.group(1).split("\\|", 2)[0].strip() : null;
    }

    private List<String> recognized(String text, List<String> vocabulary) {
        if (text == null) return List.of();
        return vocabulary.stream()
                .filter(skill -> Pattern.compile("(?<![\\w+#.])" + Pattern.quote(skill) + "(?![\\w+#.])",
                        Pattern.CASE_INSENSITIVE).matcher(text).find())
                .toList();
    }

    private List<String> items(List<String> lines) {
        if (lines == null) return List.of();
        List<String> values = new ArrayList<>();
        for (String line : lines) {
            String clean = cleanLine(line);
            if (!clean.isBlank()) {
                values.addAll(Arrays.asList(clean.split("\\s*[;|]\\s*|\\s*,\\s*")));
            }
        }
        return unique(values);
    }

    private List<String> unique(List<String> values) {
        Map<String, String> unique = new LinkedHashMap<>();
        for (String value : values) {
            String clean = value.replaceAll("^[\\s,;|]+|[\\s,;|]+$", "");
            if (!clean.isBlank()) unique.putIfAbsent(clean.toLowerCase(Locale.ROOT), clean);
        }
        return List.copyOf(unique.values());
    }

    private List<String> concat(List<String> left, List<String> right) {
        List<String> joined = new ArrayList<>(left);
        joined.addAll(right);
        return joined;
    }

    private String sectionText(List<String> lines) {
        if (lines == null) return null;
        String value = lines.stream().filter(line -> !line.isBlank()).collect(Collectors.joining(" ")).strip();
        return value.isBlank() ? null : value;
    }

    private String firstMatch(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? matcher.group().replaceAll("\\s+", " ").strip() : null;
    }

    private String cleanLine(String line) {
        return line.replaceFirst("^\\s*(?:[•*▪◦\\-–—]|\\d+[.)])\\s*", "").strip();
    }

    private static Map<String, String> sectionAliases() {
        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put("summary", "summary");
        aliases.put("professional summary", "summary");
        aliases.put("profile", "summary");
        aliases.put("professional profile", "summary");
        aliases.put("objective", "summary");
        aliases.put("career objective", "summary");
        aliases.put("experience", "experience");
        aliases.put("work experience", "experience");
        aliases.put("professional experience", "experience");
        aliases.put("employment", "experience");
        aliases.put("employment history", "experience");
        aliases.put("work history", "experience");
        aliases.put("internships", "experience");
        aliases.put("education", "education");
        aliases.put("academic background", "education");
        aliases.put("skills", "skills");
        aliases.put("technical skills", "skills");
        aliases.put("core competencies", "skills");
        aliases.put("competencies", "skills");
        aliases.put("projects", "projects");
        aliases.put("personal projects", "projects");
        aliases.put("certifications", "certifications");
        aliases.put("certificates", "certifications");
        aliases.put("licenses and certifications", "certifications");
        aliases.put("languages", "languages");
        return Map.copyOf(aliases);
    }

    private record ParsedSections(List<String> topLines, Map<String, List<String>> sections) {}
}
