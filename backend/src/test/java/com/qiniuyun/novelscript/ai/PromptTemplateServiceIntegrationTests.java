package com.qiniuyun.novelscript.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.qiniuyun.novelscript.ai.adapter.AiChatAdapter;
import com.qiniuyun.novelscript.ai.prompt.PromptTemplateService;
import java.util.Map;
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

    /**
     * 验证章节上下文 Prompt 能从类路径加载并渲染成功。
     */
    @Test
    void shouldLoadAndRenderChapterContextPromptFromClasspath() {
        String renderedPrompt = promptTemplateService.render(
            "chapter-context-extract",
            Map.of(
                "chapterNo", 1,
                "chapterTitle", "Chapter 1 Rainy Arrival",
                "wordCount", 18,
                "chapterContent", "Night presses down while Shen Yan enters the alley."
            )
        );

        assertThat(renderedPrompt).contains("1");
        assertThat(renderedPrompt).contains("Chapter 1 Rainy Arrival");
        assertThat(renderedPrompt).contains("18");
        assertThat(renderedPrompt).contains("Night presses down while Shen Yan enters the alley.");
    }

    /**
     * 验证未启用 DeepSeek 时会返回清晰异常信息。
     */
    @Test
    void shouldFailWithClearChineseMessageWhenDeepSeekIsNotEnabled() {
        assertThatThrownBy(() -> aiChatAdapter.chat("你是改编助手", "请开始分析"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("DeepSeek")
            .hasMessageContaining("未启用");
    }
}
