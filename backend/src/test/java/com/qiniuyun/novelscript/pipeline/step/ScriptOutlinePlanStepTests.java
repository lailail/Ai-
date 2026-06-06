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

    @BeforeEach
    void setUp() {
        scriptOutlinePlanStep = new ScriptOutlinePlanStep(
            aiChatAdapter,
            promptTemplateService,
            new ObjectMapper()
        );
    }

    @Test
    void test_p3_c4_outline_plan() {
        when(promptTemplateService.render(eq("script-outline-plan"), anyMap())).thenReturn("请规划剧本大纲");
        when(aiChatAdapter.chat(eq(ScriptOutlinePlanStep.SYSTEM_PROMPT), eq("请规划剧本大纲"))).thenReturn("""
            {
              "episodes": [
                {
                  "id": "ep01",
                  "title": "旧城疑影",
                  "premise": "沈砚进入旧城并接触到第一批关键线索。",
                  "source_refs": ["chapter:1", "chapter:2", "chapter:3"],
                  "scenes": [
                    {
                      "id": "sc01",
                      "slugline": "夜 外 旧城巷口",
                      "purpose": "建立悬疑氛围并引出主线线索",
                      "conflict": "沈砚犹豫是否继续深入",
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
        assertThat(episode.getScenes().get(0).getSlugline()).isEqualTo("夜 外 旧城巷口");
        assertThat(episode.getScenes().get(0).getCharacters()).containsExactly("char_shenyan");

        verify(promptTemplateService).render(eq("script-outline-plan"), anyMap());
        verify(aiChatAdapter).chat(eq(ScriptOutlinePlanStep.SYSTEM_PROMPT), eq("请规划剧本大纲"));
    }

    private StoryBibleResult buildStoryBible() {
        StoryBibleResult result = new StoryBibleResult();
        result.setProjectId(1001L);

        StoryBibleCharacter character = new StoryBibleCharacter();
        character.setId("char_shenyan");
        character.setName("沈砚");
        character.setRole("protagonist");
        character.setGoal("查明旧案真相");

        result.setCharacters(List.of(character));
        result.setAdaptationStrategy(List.of("前三章合并为第一集"));
        return result;
    }

    private List<ChapterContextResult> buildChapterContexts() {
        ChapterContextResult chapter1 = new ChapterContextResult();
        chapter1.setProjectId(1001L);
        chapter1.setChapterNo(1);
        chapter1.setChapterTitle("第一章");
        chapter1.setSummary("沈砚进入旧城。");
        chapter1.setSourceRefs(List.of("chapter:1"));

        ChapterContextResult chapter2 = new ChapterContextResult();
        chapter2.setProjectId(1001L);
        chapter2.setChapterNo(2);
        chapter2.setChapterTitle("第二章");
        chapter2.setSummary("老周带来铜牌。");
        chapter2.setSourceRefs(List.of("chapter:2"));

        ChapterContextResult chapter3 = new ChapterContextResult();
        chapter3.setProjectId(1001L);
        chapter3.setChapterNo(3);
        chapter3.setChapterTitle("第三章");
        chapter3.setSummary("林晚说出旧案。");
        chapter3.setSourceRefs(List.of("chapter:3"));

        return List.of(chapter1, chapter2, chapter3);
    }
}
