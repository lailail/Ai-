package com.qiniuyun.novelscript.pipeline.step;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiniuyun.novelscript.ai.adapter.AiChatAdapter;
import com.qiniuyun.novelscript.ai.prompt.PromptTemplateService;
import com.qiniuyun.novelscript.pipeline.model.ChapterContextResult;
import com.qiniuyun.novelscript.pipeline.model.NormalizedChapter;
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

    @BeforeEach
    void setUp() {
        chapterContextExtractStep = new ChapterContextExtractStep(
            aiChatAdapter,
            promptTemplateService,
            new ObjectMapper()
        );
    }

    @Test
    void shouldExtractStructuredContextFromAiResponse() {
        NormalizedChapter normalizedChapter = new NormalizedChapter();
        normalizedChapter.setProjectId(1001L);
        normalizedChapter.setChapterNo(1);
        normalizedChapter.setTitle("第一章 夜雨入城");
        normalizedChapter.setContent("夜色压城。沈砚第一次走进青石巷。");
        normalizedChapter.setWordCount(16);

        when(promptTemplateService.render(eq("chapter-context-extract"), anyMap())).thenReturn("请分析这一章");
        when(aiChatAdapter.chat(eq(ChapterContextExtractStep.SYSTEM_PROMPT), eq("请分析这一章"))).thenReturn("""
            {
              "summary": "沈砚雨夜入城，并在青石巷遇到可疑线索。",
              "characters": ["沈砚", "老周"],
              "locations": ["青石巷"],
              "events": ["沈砚入城", "发现异常动静"],
              "conflicts": ["是否继续追查"],
              "emotion_changes": ["警惕到犹疑"],
              "foreshadowing": ["巷口的铜牌"],
              "key_dialogues": ["老周：别回头。"],
              "source_refs": ["chapter:1"]
            }
            """);

        ChapterContextResult result = chapterContextExtractStep.execute(normalizedChapter);

        assertThat(result.getProjectId()).isEqualTo(1001L);
        assertThat(result.getChapterNo()).isEqualTo(1);
        assertThat(result.getChapterTitle()).isEqualTo("第一章 夜雨入城");
        assertThat(result.getSummary()).contains("沈砚雨夜入城");
        assertThat(result.getCharacters()).containsExactly("沈砚", "老周");
        assertThat(result.getLocations()).containsExactly("青石巷");
        assertThat(result.getEvents()).contains("沈砚入城");
        assertThat(result.getConflicts()).contains("是否继续追查");
        assertThat(result.getEmotionChanges()).contains("警惕到犹疑");
        assertThat(result.getForeshadowing()).contains("巷口的铜牌");
        assertThat(result.getKeyDialogues()).contains("老周：别回头。");
        assertThat(result.getSourceRefs()).containsExactly("chapter:1");

        verify(promptTemplateService).render(eq("chapter-context-extract"), anyMap());
        verify(aiChatAdapter).chat(eq(ChapterContextExtractStep.SYSTEM_PROMPT), eq("请分析这一章"));
    }

    @Test
    void shouldFillDefaultSourceRefWhenAiResponseDoesNotReturnSourceRefs() {
        NormalizedChapter normalizedChapter = new NormalizedChapter();
        normalizedChapter.setProjectId(1002L);
        normalizedChapter.setChapterNo(2);
        normalizedChapter.setTitle("第二章 铜牌");
        normalizedChapter.setContent("铜牌落地，老周没有回头。");
        normalizedChapter.setWordCount(13);

        when(promptTemplateService.render(eq("chapter-context-extract"), anyMap())).thenReturn("请分析第二章");
        when(aiChatAdapter.chat(eq(ChapterContextExtractStep.SYSTEM_PROMPT), eq("请分析第二章"))).thenReturn("""
            {
              "summary": "铜牌出现，疑点加深。",
              "characters": ["老周"],
              "locations": ["旧巷"]
            }
            """);

        ChapterContextResult result = chapterContextExtractStep.execute(normalizedChapter);

        assertThat(result.getSourceRefs()).containsExactly("chapter:2");
        assertThat(result.getCharacters()).containsExactly("老周");
        assertThat(result.getLocations()).containsExactly("旧巷");
        assertThat(result.getEvents()).isEqualTo(List.of());
    }
}
