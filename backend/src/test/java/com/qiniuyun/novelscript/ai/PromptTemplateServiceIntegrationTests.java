package com.qiniuyun.novelscript.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import com.qiniuyun.novelscript.ai.adapter.AiChatAdapter;
import com.qiniuyun.novelscript.ai.prompt.PromptTemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 验证 AI 提示词模板与适配器的最小可用性。
 */
@SpringBootTest
@ActiveProfiles("test")
class PromptTemplateServiceIntegrationTests {

    @Autowired
    private PromptTemplateService promptTemplateService;

    @Autowired
    private AiChatAdapter aiChatAdapter;

    @Test
    void shouldLoadAndRenderChapterContextPromptFromClasspath() {
        String renderedPrompt = promptTemplateService.render(
            "chapter-context-extract",
            Map.of(
                "chapterNo", 1,
                "chapterTitle", "第一章 夜雨入城",
                "wordCount", 18,
                "chapterContent", "夜色压城，沈砚第一次走进青石巷。"
            )
        );

        assertThat(renderedPrompt).contains("1");
        assertThat(renderedPrompt).contains("第一章 夜雨入城");
        assertThat(renderedPrompt).contains("18");
        assertThat(renderedPrompt).contains("夜色压城，沈砚第一次走进青石巷。");
    }

    @Test
    void shouldFailWithClearChineseMessageWhenDeepSeekIsNotEnabled() {
        assertThatThrownBy(() -> aiChatAdapter.chat("你是改编助手", "请开始分析"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("DeepSeek")
            .hasMessageContaining("未启用");
    }
}
