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
 * 验证 AI 基础设施在未接入真实模型前的最小可用性。
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
                "chapterTitle", "第一章 夜雨入城",
                "chapterContent", "夜色压城，沈砚第一次走进青石巷。"
            )
        );

        assertThat(renderedPrompt).contains("第一章 夜雨入城");
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
