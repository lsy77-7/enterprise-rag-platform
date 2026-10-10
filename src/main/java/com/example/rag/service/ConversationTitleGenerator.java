package com.example.rag.service;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class ConversationTitleGenerator {

    private static final int MAX_AUTO_TITLE_LENGTH = 50;

    private static final Pattern PUNCTUATION = Pattern.compile(
            "[\\s，。！？、；：\"'“”‘’()（）\\[\\]【】《》<>]+"
    );

    public String generate(String question) {
        if (question == null || question.isBlank()) {
            return null;
        }

        String source = question.strip();
        String comparisonTitle = extractComparisonTitle(source);
        if (comparisonTitle != null) {
            return fitLength(comparisonTitle);
        }

        String text = source
                .replace("有什么区别", "")
                .replace("区别是什么", "")
                .replace("请问", "")
                .replace("请", "")
                .replace("帮我", "")
                .replace("总结一下", "")
                .replace("介绍一下", "")
                .replace("什么是", "")
                .replace("是什么", "")
                .replace("有哪些", "")
                .replace("如何", "")
                .replace("怎么", "")
                .replace("为什么", "")
                .replace("这份", "")
                .replace("这个", "")
                .replace("这篇", "")
                .replace("文档", "")
                .replace("PDF", "")
                .replace("中的", "")
                .replace("的内容", "")
                .replace("内容", "")
                .replace("？", "")
                .replace("?", "")
                .replace("吗", "")
                .replace("呢", "");

        text = PUNCTUATION.matcher(text).replaceAll(" ").strip();
        if (text.length() < 4) {
            text = source.replaceAll("[？?。！!，,]+", "").strip();
        }
        return fitLength(text);
    }

    private String extractComparisonTitle(String source) {
        String normalized = source.replace("？", "").replace("?", "").strip();
        if (!normalized.contains("区别")) {
            return null;
        }

        String core = normalized
                .replace("有什么区别", "")
                .replace("区别是什么", "")
                .replace("的区别", "")
                .strip();

        String separator = null;
        if (core.contains("以及")) {
            separator = "以及";
        } else if (core.contains("与")) {
            separator = "与";
        } else if (core.contains("和")) {
            separator = "和";
        }
        if (separator == null) {
            return null;
        }

        int separatorIndex = core.lastIndexOf(separator);
        String left = cleanPart(core.substring(0, separatorIndex)).replace("中的", "的");
        String right = cleanPart(core.substring(separatorIndex + separator.length()));
        if (left.isBlank() || right.isBlank()) {
            return null;
        }
        return left + " 与 " + right;
    }

    private String cleanPart(String value) {
        return PUNCTUATION.matcher(value).replaceAll(" ").strip();
    }

    private String fitLength(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        if (trimmed.isBlank()) {
            return null;
        }
        if (trimmed.length() > MAX_AUTO_TITLE_LENGTH) {
            return trimmed.substring(0, MAX_AUTO_TITLE_LENGTH);
        }
        return trimmed;
    }
}
