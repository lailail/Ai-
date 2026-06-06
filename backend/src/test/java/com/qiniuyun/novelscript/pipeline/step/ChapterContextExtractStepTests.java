package com.qiniuyun.novelscript.pipeline.step;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiniuyun.novelscript.ai.adapter.AiChatAdapter;
import com.qiniuyun.novelscript.ai.prompt.PromptTemplateService;
import com.qiniuyun.novelscript.pipeline.model.ChapterContextResult;
import com.qiniuyun.novelscript.pipeline.model.NormalizedChapter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 验证单章上下文抽取步骤的结构化解析行为。
 */
@ExtendWith(MockitoExtension.class)
class ChapterContextExtractStepTests {

    @Mock
    private AiChatAdapter aiChatAdapter;

    @Mock
    private PromptTemplateService promptTemplateService;

    private ChapterContextExtractStep chapterContextExtractStep;

    /**
     * 初始化单章上下文抽取步骤实例。
     */
    @BeforeEach
    void setUp() {
        chapterContextExtractStep = new ChapterContextExtractStep(
            aiChatAdapter,
            promptTemplateService,
            new ObjectMapper()
        );
    }

    /**
     * 验证模型返回完整 JSON 时可以正确解析为单章上下文。
     */
    @Test
    void shouldExtractStructuredContextFromAiResponse() {
        NormalizedChapter normalizedChapter = new NormalizedChapter();
        normalizedChapter.setProjectId(1001L);
        normalizedChapter.setChapterNo(1);
        normalizedChapter.setTitle("Chapter 1 Rainy Arrival");
        normalizedChapter.setContent("Night presses down. Shen Yan steps into the alley.");
        normalizedChapter.setWordCount(16);

        when(promptTemplateService.render(eq("chapter-context-extract"), anyMap())).thenReturn("analyze chapter");
        when(aiChatAdapter.chat(eq(ChapterContextExtractStep.SYSTEM_PROMPT), eq("analyze chapter"))).thenReturn("""
            {
              "summary": "Shen Yan enters the old town at night and notices something unusual.",
              "characters": ["Shen Yan", "Lao Zhou"],
              "locations": ["Stone Alley"],
              "events": ["Shen Yan enters town", "He spots unusual movement"],
              "conflicts": ["Should he keep investigating"],
              "emotion_changes": ["From alert to suspicious"],
              "foreshadowing": ["A bronze token at the alley entrance"],
              "key_dialogues": ["Lao Zhou: Do not look back."],
              "source_refs": ["chapter:1"]
            }
            """);

        ChapterContextResult result = chapterContextExtractStep.execute(normalizedChapter);

        assertThat(result.getProjectId()).isEqualTo(1001L);
        assertThat(result.getChapterNo()).isEqualTo(1);
        assertThat(result.getChapterTitle()).isEqualTo("Chapter 1 Rainy Arrival");
        assertThat(result.getSummary()).contains("Shen Yan enters");
        assertThat(result.getCharacters()).containsExactly("Shen Yan", "Lao Zhou");
        assertThat(result.getLocations()).containsExactly("Stone Alley");
        assertThat(result.getEvents()).contains("Shen Yan enters town");
        assertThat(result.getConflicts()).contains("Should he keep investigating");
        assertThat(result.getEmotionChanges()).contains("From alert to suspicious");
        assertThat(result.getForeshadowing()).contains("A bronze token at the alley entrance");
        assertThat(result.getKeyDialogues()).contains("Lao Zhou: Do not look back.");
        assertThat(result.getSourceRefs()).containsExactly("chapter:1");

        verify(promptTemplateService).render(eq("chapter-context-extract"), anyMap());
        verify(aiChatAdapter).chat(eq(ChapterContextExtractStep.SYSTEM_PROMPT), eq("analyze chapter"));
    }

    /**
     * 验证模型未返回 source_refs 时会自动补齐默认章节引用。
     */
    @Test
    void shouldFillDefaultSourceRefWhenAiResponseDoesNotReturnSourceRefs() {
        NormalizedChapter normalizedChapter = new NormalizedChapter();
        normalizedChapter.setProjectId(1002L);
        normalizedChapter.setChapterNo(2);
        normalizedChapter.setTitle("Chapter 2 Token");
        normalizedChapter.setContent("The token falls and Lao Zhou does not look back.");
        normalizedChapter.setWordCount(13);

        when(promptTemplateService.render(eq("chapter-context-extract"), anyMap())).thenReturn("analyze second chapter");
        when(aiChatAdapter.chat(eq(ChapterContextExtractStep.SYSTEM_PROMPT), eq("analyze second chapter"))).thenReturn("""
            {
              "summary": "The token appears and the mystery deepens.",
              "characters": ["Lao Zhou"],
              "locations": ["Old Alley"]
            }
            """);

        ChapterContextResult result = chapterContextExtractStep.execute(normalizedChapter);

        assertThat(result.getSourceRefs()).containsExactly("chapter:2");
        assertThat(result.getCharacters()).containsExactly("Lao Zhou");
        assertThat(result.getLocations()).containsExactly("Old Alley");
        assertThat(result.getEvents()).isEqualTo(List.of());
    }
}
