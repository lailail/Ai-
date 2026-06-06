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
import com.qiniuyun.novelscript.pipeline.model.GlobalContextMergeResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 验证全局上下文合并步骤的结构化解析行为。
 */
@ExtendWith(MockitoExtension.class)
class GlobalContextMergeStepTests {

    @Mock
    private AiChatAdapter aiChatAdapter;

    @Mock
    private PromptTemplateService promptTemplateService;

    private GlobalContextMergeStep globalContextMergeStep;

    @BeforeEach
    void setUp() {
        globalContextMergeStep = new GlobalContextMergeStep(
            aiChatAdapter,
            promptTemplateService,
            new ObjectMapper()
        );
    }

    @Test
    void test_p3_c3_global_context_merge() {
        when(promptTemplateService.render(eq("global-context-merge"), anyMap())).thenReturn("请合并章节上下文");
        when(aiChatAdapter.chat(eq(GlobalContextMergeStep.SYSTEM_PROMPT), eq("请合并章节上下文"))).thenReturn("""
            {
              "summary": "沈砚在旧城调查连环线索，核心矛盾逐步浮出水面。",
              "characters": ["沈砚", "老周", "林晚"],
              "locations": ["青石巷", "旧城码头"],
              "timeline": ["沈砚入城", "发现铜牌", "林晚提供旧案线索"],
              "relationships": ["沈砚与老周互相试探", "沈砚与林晚形成临时同盟"],
              "conflicts": ["是否继续追查旧案", "线索真假难辨"],
              "source_context_refs": ["chapter:1", "chapter:2", "chapter:3"]
            }
            """);

        GlobalContextMergeResult result = globalContextMergeStep.execute(1001L, buildChapterContexts());

        assertThat(result.getProjectId()).isEqualTo(1001L);
        assertThat(result.getSummary()).contains("沈砚");
        assertThat(result.getCharacters()).containsExactly("沈砚", "老周", "林晚");
        assertThat(result.getLocations()).containsExactly("青石巷", "旧城码头");
        assertThat(result.getTimeline()).contains("发现铜牌");
        assertThat(result.getRelationships()).contains("沈砚与林晚形成临时同盟");
        assertThat(result.getConflicts()).contains("线索真假难辨");
        assertThat(result.getSourceContextRefs()).containsExactly("chapter:1", "chapter:2", "chapter:3");

        verify(promptTemplateService).render(eq("global-context-merge"), anyMap());
        verify(aiChatAdapter).chat(eq(GlobalContextMergeStep.SYSTEM_PROMPT), eq("请合并章节上下文"));
    }

    private List<ChapterContextResult> buildChapterContexts() {
        return List.of(
            createChapterContext(1001L, 1, "第一章", "沈砚进入青石巷。", List.of("沈砚"), List.of("青石巷")),
            createChapterContext(1001L, 2, "第二章", "老周带来铜牌。", List.of("老周"), List.of("旧城码头")),
            createChapterContext(1001L, 3, "第三章", "林晚说出旧案。", List.of("林晚"), List.of("旧城茶馆"))
        );
    }

    private ChapterContextResult createChapterContext(
        Long projectId,
        Integer chapterNo,
        String chapterTitle,
        String summary,
        List<String> characters,
        List<String> locations
    ) {
        ChapterContextResult result = new ChapterContextResult();
        result.setProjectId(projectId);
        result.setChapterNo(chapterNo);
        result.setChapterTitle(chapterTitle);
        result.setSummary(summary);
        result.setCharacters(characters);
        result.setLocations(locations);
        result.setEvents(List.of(summary));
        result.setSourceRefs(List.of("chapter:" + chapterNo));
        return result;
    }
}
