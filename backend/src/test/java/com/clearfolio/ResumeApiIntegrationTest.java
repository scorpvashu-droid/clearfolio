package com.clearfolio;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ResumeApiIntegrationTest {
    private static final Path DATABASE = Path.of("target", "test-data", UUID.randomUUID() + ".sqlite3");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void databasePath(DynamicPropertyRegistry registry) {
        registry.add("DATABASE_PATH", () -> DATABASE.toString());
        registry.add("MAX_UPLOAD_MB", () -> "1");
        registry.add("FRONTEND_ORIGIN", () -> "http://localhost:5173");
    }

    @BeforeEach
    void clearHistory() {
        jdbcTemplate.update("DELETE FROM resumes");
    }

    @Test
    void healthAndCorsAreAvailable() throws Exception {
        mockMvc.perform(get("/api/health").header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void parsesPdfAndSupportsHistoryReadAndDelete() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile(
                "file", "resume.pdf", MediaType.APPLICATION_PDF_VALUE, createPdf(
                        "Jordan Lee\njordan@example.com | +1 415 555 0198 | Austin, TX\n"
                                + "Professional Summary\nBackend engineer with 4 years of experience.\n"
                                + "Skills\nPython, FastAPI, communication\n"
                                + "Experience\nSoftware Engineer at Example Inc. 2021 - Present\nBuilt APIs."));
        String response = mockMvc.perform(multipart("/api/resumes/parse").file(pdf))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filename").value("resume.pdf"))
                .andExpect(jsonPath("$.data.name").value("Jordan Lee"))
                .andExpect(jsonPath("$.data.email").value("jordan@example.com"))
                .andExpect(jsonPath("$.data.location").value("Austin, TX"))
                .andExpect(jsonPath("$.data.experiences[0].job_title").value("Software Engineer"))
                .andReturn().getResponse().getContentAsString();
        String id = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(response).get("id").asText();

        mockMvc.perform(get("/api/resumes").param("page", "1").param("page_size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(id));
        mockMvc.perform(get("/api/resumes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
        mockMvc.perform(delete("/api/resumes/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/resumes/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void parsesDocxAndLeavesMissingFieldsNullOrEmpty() throws Exception {
        MockMultipartFile docx = new MockMultipartFile(
                "file",
                "sparse.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                createDocx("Taylor\nSkills\nJava"));
        mockMvc.perform(multipart("/api/resumes/parse").file(docx))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Taylor"))
                .andExpect(jsonPath("$.data.email").doesNotExist())
                .andExpect(jsonPath("$.data.experiences").isEmpty())
                .andExpect(jsonPath("$.data.technical_skills[0]").value("Java"));
    }

    @Test
    void rejectsInvalidUploadsAndScannedPdfs() throws Exception {
        mockMvc.perform(multipart("/api/resumes/parse").file(
                        new MockMultipartFile("file", "resume.exe", "application/octet-stream", new byte[] {1})))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("PDF or DOCX")));
        mockMvc.perform(multipart("/api/resumes/parse").file(
                        new MockMultipartFile("file", "resume.pdf", MediaType.TEXT_PLAIN_VALUE, new byte[] {1})))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("MIME type")));
        mockMvc.perform(multipart("/api/resumes/parse").file(
                        new MockMultipartFile("file", "empty.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[0])))
                .andExpect(status().isBadRequest());
        mockMvc.perform(multipart("/api/resumes/parse").file(
                        new MockMultipartFile("file", "broken.pdf", MediaType.APPLICATION_PDF_VALUE,
                                "%PDF-not-valid".getBytes())))
                        .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(multipart("/api/resumes/parse").file(
                        new MockMultipartFile("file", "scanned.pdf", MediaType.APPLICATION_PDF_VALUE, createPdf(""))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(containsString("OCR is required")));
        mockMvc.perform(multipart("/api/resumes/parse").file(
                        new MockMultipartFile("file", "large.docx", "application/octet-stream",
                                new byte[1024 * 1024 + 1])))
                .andExpect(status().isPayloadTooLarge());
        mockMvc.perform(multipart("/api/resumes/parse").file(
                        new MockMultipartFile("file", "broken.docx",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                createDocxArchive("word/document.xml", "<bad xml"))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void validatesPaginationAndMissingRecords() throws Exception {
        mockMvc.perform(get("/api/resumes").param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists());
        mockMvc.perform(get("/api/resumes/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/resumes/not-a-uuid"))
                .andExpect(status().isNotFound());
    }

    private byte[] createPdf(String text) throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            if (!text.isBlank()) {
                try (PDPageContentStream stream = new PDPageContentStream(document, document.getPage(0))) {
                    stream.beginText();
                    stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                    stream.newLineAtOffset(50, 750);
                    for (String line : text.split("\\n")) {
                        stream.showText(line);
                        stream.newLineAtOffset(0, -16);
                    }
                    stream.endText();
                }
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private byte[] createDocx(String text) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            addZipEntry(zip, "[Content_Types].xml",
                    "<?xml version=\"1.0\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                            + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                            + "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>"
                            + "</Types>");
            addZipEntry(zip, "_rels/.rels",
                    "<?xml version=\"1.0\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>"
                            + "</Relationships>");
            String paragraphs = java.util.Arrays.stream(text.split("\\n"))
                    .map(line -> "<w:p><w:r><w:t>" + xmlEscape(line) + "</w:t></w:r></w:p>")
                    .reduce("", String::concat);
            addZipEntry(zip, "word/document.xml",
                    "<?xml version=\"1.0\"?><w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
                            + "<w:body>" + paragraphs + "<w:sectPr/></w:body></w:document>");
        }
        return output.toByteArray();
    }

    private byte[] createDocxArchive(String name, String content) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            addZipEntry(zip, name, content);
        }
        return output.toByteArray();
    }

    private void addZipEntry(ZipOutputStream zip, String name, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String xmlEscape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
