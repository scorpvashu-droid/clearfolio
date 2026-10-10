package com.clearfolio.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.zip.ZipInputStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

@Service
public class DocumentTextExtractor {
    private static final String DOCX_DOCUMENT = "word/document.xml";
    private static final int MAX_DOCX_ENTRIES = 2_000;
    private static final long MAX_DOCX_EXPANDED_BYTES = 50L * 1024 * 1024;

    public String extract(byte[] content, String extension) {
        try {
            String text = extension.equals(".pdf") ? extractPdf(content) : extractDocx(content);
            if (text.isBlank()) {
                if (extension.equals(".pdf")) {
                    throw new ResumeParseException(
                            422, "This PDF has no extractable text. It may be scanned; OCR is required.");
                }
                throw new ResumeParseException(422, "No readable text was found in the resume.");
            }
            return text;
        } catch (ResumeParseException exception) {
            throw exception;
        } catch (IOException | InvalidFormatException | IllegalArgumentException | POIXMLException exception) {
            throw new ResumeParseException(
                    422, "The document could not be read. Check that it is a valid PDF or DOCX with readable text.");
        }
    }

    private String extractPdf(byte[] content) throws IOException {
        try (PDDocument document = Loader.loadPDF(content)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String extractDocx(byte[] content) throws IOException, InvalidFormatException {
        validateDocxArchive(content);
        ZipSecureFile.setMaxEntrySize(MAX_DOCX_EXPANDED_BYTES);
        ZipSecureFile.setMaxTextSize(10L * 1024 * 1024);
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(content))) {
            StringBuilder text = new StringBuilder();
            document.getParagraphs().forEach(paragraph -> appendLine(text, paragraph.getText()));
            document.getTables().forEach(table -> table.getRows().forEach(row ->
                    row.getTableCells().forEach(cell -> appendLine(text, cell.getText()))));
            return text.toString();
        }
    }

    private void validateDocxArchive(byte[] content) throws IOException {
        int entryCount = 0;
        long expandedSize = 0;
        boolean hasDocument = false;
        byte[] buffer = new byte[8192];
        try (ZipInputStream archive = new ZipInputStream(new ByteArrayInputStream(content))) {
            java.util.zip.ZipEntry entry;
            while ((entry = archive.getNextEntry()) != null) {
                entryCount++;
                if (entryCount > MAX_DOCX_ENTRIES) {
                    throw new ResumeParseException(400, "The DOCX document is not valid.");
                }
                if (DOCX_DOCUMENT.equals(entry.getName())) {
                    hasDocument = true;
                }
                int bytesRead;
                while ((bytesRead = archive.read(buffer)) != -1) {
                    expandedSize += bytesRead;
                    if (expandedSize > MAX_DOCX_EXPANDED_BYTES) {
                        throw new ResumeParseException(400, "The DOCX document is not valid.");
                    }
                }
                archive.closeEntry();
            }
        }
        if (!hasDocument) {
            throw new ResumeParseException(400, "The file is not a valid DOCX document.");
        }
    }

    private void appendLine(StringBuilder text, String value) {
        if (value != null && !value.isBlank()) {
            if (!text.isEmpty()) text.append('\n');
            text.append(value.strip());
        }
    }

    public static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot).toLowerCase(Locale.ROOT);
    }
}
