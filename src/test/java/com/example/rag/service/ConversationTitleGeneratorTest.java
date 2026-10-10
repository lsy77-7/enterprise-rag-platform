package com.example.rag.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConversationTitleGeneratorTest {

    private final ConversationTitleGenerator generator = new ConversationTitleGenerator();

    @Test
    void shouldGenerateTitleFromQuestion() {
        String title = generator.generate("帮我总结一下这份 PDF 中的康复训练方案");

        assertNotNull(title);
        assertEquals(true, title.length() >= 4 && title.length() <= 20);
    }

    @Test
    void shouldHandleComparisonQuestion() {
        String title = generator.generate("Spring Boot 中的 IoC 和 AOP 有什么区别？");

        assertEquals("Spring Boot 的 IoC 与 AOP", title);
    }

    @Test
    void shouldKeepTitleShort() {
        String title = generator.generate("请详细介绍企业知识库中 RAG 检索流程的具体实现方式以及实际应用场景");

        assertNotNull(title);
        assertEquals(true, title.length() <= 50);
    }
}
