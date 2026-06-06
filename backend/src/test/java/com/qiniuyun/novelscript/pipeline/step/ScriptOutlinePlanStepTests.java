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
import com.qiniuyun.novelscript.pipeline.model.ScriptOutlineEpisode;
import com.qiniuyun.novelscript.pipeline.model.ScriptOutlineResult;
import com.qiniuyun.novelscript.pipeline.model.StoryBibleCharacter;
import com.qiniuyun.novelscript.pipeline.model.StoryBibleResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 验证剧本大纲规划步骤的结构化解析行为。
 */
@ExtendWith(MockitoExtension.class)
class ScriptOutlinePlanStepTests {

    @Mock
    private AiChatAdapter aiChatAdapter;

    @Mock
    private PromptTemplateService promptTemplateService;

    private ScriptOutlinePlanStep scriptOutlinePlanStep;

    /**
     * 初始化剧本大纲规划步骤实例。
     */
    @BeforeEach
    void setUp() {
        scriptOutlinePlanStep = new ScriptOutlinePlanStep(
            aiChatAdapter,
            promptTemplateService,
            new ObjectMapper()
        );
    }

    /**
     * 验证 Story Bible 与章节上下文能够被规划成剧本大纲。
     */
    @Test
    void test_p3_c4_outline_plan() {
        when(promptTemplateService.render(eq("script-outline-plan"), anyMap())).thenReturn("plan outline");
        when(aiChatAdapter.chat(eq(ScriptOutlinePlanStep.SYSTEM_PROMPT), eq("plan outline"))).thenReturn("""
            {
              "episodes": [
                {
                  "id": "ep01",
                  "title": "Shadow Over The Old Town",
                  "premise": "Shen Yan enters the old town and touches the first key clues.",
                  "source_refs": ["chapter:1", "chapter:2", "chapter:3"],
                  "scenes": [
                    {
                      "id": "sc01",
                      "slugline": "EXT. OLD TOWN ALLEY - NIGHT",
                      "purpose": "Set the suspense tone and introduce the main clue",
                      "conflict": "Shen Yan is unsure whether to go deeper",
                      "source_refs": ["chapter:1"],
                      "characters": ["char_shenyan"]
                    }
                  ]
                }
              ]
            }
            """);

        ScriptOutlineResult result = scriptOutlinePlanStep.execute(1001L, buildStoryBible(), buildChapterContexts());

        assertThat(result.getProjectId()).isEqualTo(1001L);
        assertThat(result.getEpisodes()).hasSize(1);
        ScriptOutlineEpisode episode = result.getEpisodes().get(0);
        assertThat(episode.getId()).isEqualTo("ep01");
        assertThat(episode.getScenes()).hasSize(1);
        assertThat(episode.getScenes().get(0).getSlugline()).isEqualTo("EXT. OLD TOWN ALLEY - NIGHT");
        assertThat(episode.getScenes().get(0).getCharacters()).containsExactly("char_shenyan");

        verify(promptTemplateService).render(eq("script-outline-plan"), anyMap());
        verify(aiChatAdapter).chat(eq(ScriptOutlinePlanStep.SYSTEM_PROMPT), eq("plan outline"));
    }

    /**
     * 构造测试用 Story Bible 结果。
     *
     * @return Story Bible 结果
     */
    private StoryBibleResult buildStoryBible() {
        StoryBibleResult result = new StoryBibleResult();
        result.setProjectId(1001L);

        StoryBibleCharacter character = new StoryBibleCharacter();
        character.setId("char_shenyan");
        character.setName("Shen Yan");
        character.setRole("protagonist");
        character.setGoal("Find the truth of the old case");

        result.setCharacters(List.of(character));
        result.setAdaptationStrategy(List.of("Merge the first three chapters into episode one"));
        return result;
    }

    /**
     * 构造测试用章节上下文列表。
     *
     * @return 章节上下文列表
     */
    private List<ChapterContextResult> buildChapterContexts() {
        ChapterContextResult chapter1 = new ChapterContextResult();
        chapter1.setProjectId(1001L);
        chapter1.setChapterNo(1);
        chapter1.setChapterTitle("Chapter 1");
        chapter1.setSummary("Shen Yan enters the old town.");
        chapter1.setSourceRefs(List.of("chapter:1"));

        ChapterContextResult chapter2 = new ChapterContextResult();
        chapter2.setProjectId(1001L);
        chapter2.setChapterNo(2);
        chapter2.setChapterTitle("Chapter 2");
        chapter2.setSummary("Lao Zhou brings the token.");
        chapter2.setSourceRefs(List.of("chapter:2"));

        ChapterContextResult chapter3 = new ChapterContextResult();
        chapter3.setProjectId(1001L);
        chapter3.setChapterNo(3);
        chapter3.setChapterTitle("Chapter 3");
        chapter3.setSummary("Lin Wan reveals the old case.");
        chapter3.setSourceRefs(List.of("chapter:3"));

        return List.of(chapter1, chapter2, chapter3);
    }
}
