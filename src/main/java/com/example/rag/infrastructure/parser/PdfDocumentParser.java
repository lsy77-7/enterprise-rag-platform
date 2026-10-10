package com.example.rag.infrastructure.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfDocumentParser {

    public ParsedDocument parse(Path filePath) {
        if (filePath == null || !Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException("PDF 文件不存在: " + filePath);
        }

        try (PDDocument document = Loader.loadPDF(filePath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            int pageCount = document.getNumberOfPages();
            List<ParsedPage> pages = new ArrayList<>();

            for (int pageNumber = 1; pageNumber <= pageCount; pageNumber++) {
                stripper.setStartPage(pageNumber);
                stripper.setEndPage(pageNumber);
                String text = normalize(stripper.getText(document));

                if (!text.isBlank()) {
                    pages.add(new ParsedPage(pageNumber, text));
                }
            }

            if (pages.isEmpty()) {
                throw new IllegalStateException("PDF 未提取到任何文本");
            }

            return new ParsedDocument(pages);
        } catch (IOException ex) {
            throw new IllegalStateException("PDF 解析失败: " + ex.getMessage(), ex);
        }
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }

        String normalized = text
                .replace("\r\n", "\n")
                .replace('\r', '\n');

        normalized = normalized.replaceAll("[ \\t\\u000B\\f]+", " ");
        normalized = normalized.replaceAll("(?m)^[ \\t]+|[ \\t]+$", "");
        normalized = normalized.replaceAll("\\n{3,}", "\n\n");
        return normalized.strip();
    }
}
