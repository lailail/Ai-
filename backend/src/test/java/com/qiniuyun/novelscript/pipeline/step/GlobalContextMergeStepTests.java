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
import com.qiniuyun.novelscript.pipeline.model.GlobalContextMergeResult;
import java.util.List;
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

    /**
     * 初始化全局上下文合并步骤实例。
     */
    @BeforeEach
    void setUp() {
        globalContextMergeStep = new GlobalContextMergeStep(
            aiChatAdapter,
            promptTemplateService,
            new ObjectMapper()
        );
    }

    /**
     * 验证多章上下文能够被正确合并为全局上下文结果。
     */
    @Test
    void test_p3_c3_global_context_merge() {
        when(promptTemplateService.render(eq("global-context-merge"), anyMap())).thenReturn("merge contexts");
        when(aiChatAdapter.chat(eq(GlobalContextMergeStep.SYSTEM_PROMPT), eq("merge contexts"))).thenReturn("""
            {
              "summary": "Shen Yan follows the clues across the old town and the core conflict gradually surfaces.",
              "characters": ["Shen Yan", "Lao Zhou", "Lin Wan"],
              "locations": ["Stone Alley", "Old Wharf"],
              "timeline": ["Shen Yan enters town", "The bronze token appears", "Lin Wan reveals the old case clue"],
              "relationships": ["Shen Yan and Lao Zhou test each other", "Shen Yan and Lin Wan form a temporary alliance"],
              "conflicts": ["Should he keep investigating the old case", "The clues may be misleading"],
              "source_context_refs": ["chapter:1", "chapter:2", "chapter:3"]
            }
            """);

        GlobalContextMergeResult result = globalContextMergeStep.execute(1001L, buildChapterContexts());

        assertThat(result.getProjectId()).isEqualTo(1001L);
        assertThat(result.getSummary()).contains("Shen Yan");
        assertThat(result.getCharacters()).containsExactly("Shen Yan", "Lao Zhou", "Lin Wan");
        assertThat(result.getLocations()).containsExactly("Stone Alley", "Old Wharf");
        assertThat(result.getTimeline()).contains("The bronze token appears");
        assertThat(result.getRelationships()).contains("Shen Yan and Lin Wan form a temporary alliance");
        assertThat(result.getConflicts()).contains("The clues may be misleading");
        assertThat(result.getSourceContextRefs()).containsExactly("chapter:1", "chapter:2", "chapter:3");

        verify(promptTemplateService).render(eq("global-context-merge"), anyMap());
        verify(aiChatAdapter).chat(eq(GlobalContextMergeStep.SYSTEM_PROMPT), eq("merge contexts"));
    }

    /**
     * 构造测试用章节上下文列表。
     *
     * @return 章节上下文列表
     */
    private List<ChapterContextResult> buildChapterContexts() {
        return List.of(
            createChapterContext(1001L, 1, "Chapter 1", "Shen Yan enters Stone Alley.", List.of("Shen Yan"), List.of("Stone Alley")),
            createChapterContext(1001L, 2, "Chapter 2", "Lao Zhou brings the token.", List.of("Lao Zhou"), List.of("Old Wharf")),
            createChapterContext(1001L, 3, "Chapter 3", "Lin Wan explains the old case.", List.of("Lin Wan"), List.of("Tea House"))
        );
    }

    /**
     * 构造单条章节上下文测试数据。
     *
     * @param projectId 项目 ID
     * @param chapterNo 章节号
     * @param chapterTitle 章节标题
     * @param summary 章节摘要
     * @param characters 人物列表
     * @param locations 地点列表
     * @return 章节上下文结果
     */
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
