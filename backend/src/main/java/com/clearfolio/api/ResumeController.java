package com.clearfolio.api;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.clearfolio.model.HealthResponse;
import com.clearfolio.model.ResumeData;
import com.clearfolio.model.ResumePage;
import com.clearfolio.model.ResumeRecord;
import com.clearfolio.service.DocumentTextExtractor;
import com.clearfolio.service.ResumeHistoryService;
import com.clearfolio.service.ResumeParseException;
import com.clearfolio.service.ResumeTextParser;

@Validated
@RestController
@RequestMapping("/api")
public class ResumeController {
    private static final String PDF_MIME = "application/pdf";
    private static final String DOCX_MIME =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final Pattern CONTROL_CHARACTERS = Pattern.compile("[\\x00-\\x1f\\x7f]");

    private final ResumeTextParser parser;
    private final DocumentTextExtractor extractor;
    private final ResumeHistoryService history;
    private final long maxUploadBytes;

    public ResumeController(
            ResumeTextParser parser,
            DocumentTextExtractor extractor,
            ResumeHistoryService history,
            @Value("${MAX_UPLOAD_MB:10}") int maxUploadMb) {
        if (maxUploadMb < 1 || maxUploadMb > 100) {
            throw new IllegalArgumentException("MAX_UPLOAD_MB must be between 1 and 100.");
        }
        this.parser = parser;
        this.extractor = extractor;
        this.history = history;
        this.maxUploadBytes = maxUploadMb * 1024L * 1024L;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("ok");
    }

    @PostMapping(path = "/resumes/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeRecord> parse(@RequestParam("file") MultipartFile file) {
        String filename = safeFilename(file.getOriginalFilename());
        String extension = DocumentTextExtractor.extensionOf(filename);
        if (!extension.equals(".pdf") && !extension.equals(".docx")) {
            throw new ResumeParseException(400, "Upload a PDF or DOCX resume.");
        }
        validateMimeType(file.getContentType(), extension);
        if (file.isEmpty()) {
            throw new ResumeParseException(400, "The uploaded file is empty.");
        }
        if (file.getSize() > maxUploadBytes) {
            throw new ResumeParseException(413, "The uploaded file exceeds the configured size limit.");
        }

        try {
            byte[] content = file.getBytes();
            if (extension.equals(".pdf") && !startsWithPdfSignature(content)) {
                throw new ResumeParseException(400, "The file is not a valid PDF document.");
            }
            ResumeData data = parser.parse(extractor.extract(content, extension));
            return ResponseEntity.status(HttpStatus.CREATED).body(history.save(filename, data));
        } catch (ResumeParseException exception) {
            throw exception;
        } catch (java.io.IOException exception) {
            throw new ResumeParseException(422, "The uploaded document could not be read.");
        }
    }

    @GetMapping("/resumes")
    public ResumePage list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(name = "page_size", defaultValue = "10") @Min(1) @Max(100) int pageSize) {
        return history.list(page, pageSize);
    }

    @GetMapping("/resumes/{resumeId}")
    public ResumeRecord get(@PathVariable String resumeId) {
        UUID id = parseResumeId(resumeId);
        ResumeRecord record = history.get(id);
        if (record == null) {
            throw new ResumeParseException(404, "Resume not found.");
        }
        return record;
    }

    @DeleteMapping("/resumes/{resumeId}")
    public ResponseEntity<Void> delete(@PathVariable String resumeId) {
        if (!history.delete(parseResumeId(resumeId))) {
            throw new ResumeParseException(404, "Resume not found.");
        }
        return ResponseEntity.noContent().build();
    }

    private UUID parseResumeId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new ResumeParseException(404, "Resume not found.");
        }
    }

    private String safeFilename(String originalFilename) {
        String name = originalFilename == null ? "" : originalFilename.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        name = name.substring(slash + 1);
        name = CONTROL_CHARACTERS.matcher(name).replaceAll("").strip();
        if (name.length() > 255) name = name.substring(0, 255);
        return name;
    }

    private void validateMimeType(String mimeType, String extension) {
        String normalized = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        boolean matches = normalized.equals("application/octet-stream")
                || (extension.equals(".pdf") && normalized.equals(PDF_MIME))
                || (extension.equals(".docx") && normalized.equals(DOCX_MIME));
        if (!matches) {
            throw new ResumeParseException(400, "The uploaded file's MIME type does not match its extension.");
        }
    }

    private boolean startsWithPdfSignature(byte[] content) {
        byte[] signature = "%PDF-".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        if (content.length < signature.length) return false;
        for (int index = 0; index < signature.length; index++) {
            if (content[index] != signature[index]) return false;
        }
        return true;
    }
}
