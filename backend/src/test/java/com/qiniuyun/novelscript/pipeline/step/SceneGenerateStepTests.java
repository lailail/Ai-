package com.qiniuyun.novelscript.pipeline.step;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiniuyun.novelscript.ai.adapter.AiChatAdapter;
import com.qiniuyun.novelscript.ai.prompt.PromptTemplateService;
import com.qiniuyun.novelscript.pipeline.model.ScriptOutlineScene;
import com.qiniuyun.novelscript.pipeline.model.ScriptSceneBeat;
import com.qiniuyun.novelscript.pipeline.model.ScriptSceneDialogue;
import com.qiniuyun.novelscript.pipeline.model.ScriptSceneResult;
import com.qiniuyun.novelscript.pipeline.model.StoryBibleCharacter;
import com.qiniuyun.novelscript.pipeline.model.StoryBibleResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 验证场景生成步骤的结构化解析行为。
 */
@ExtendWith(MockitoExtension.class)
class SceneGenerateStepTests {

    @Mock
    private AiChatAdapter aiChatAdapter;

    @Mock
    private PromptTemplateService promptTemplateService;

    private SceneGenerateStep sceneGenerateStep;

    @BeforeEach
    void setUp() {
        sceneGenerateStep = new SceneGenerateStep(
            aiChatAdapter,
            promptTemplateService,
            new ObjectMapper()
        );
    }

    @Test
    void test_p3_c4_scene_generate() {
        when(promptTemplateService.render(eq("scene-generate"), anyMap())).thenReturn("请生成场景");
        when(aiChatAdapter.chat(eq(SceneGenerateStep.SYSTEM_PROMPT), eq("请生成场景"))).thenReturn("""
            {
              "id": "sc01",
              "slugline": "夜 外 旧城巷口",
              "purpose": "建立悬疑氛围并引出主线线索",
              "source_refs": ["chapter:1"],
              "characters": ["char_shenyan"],
              "actions": ["沈砚停在巷口，仔细观察墙角血迹。"],
              "beats": [
                {
                  "id": "beat01",
                  "action": "沈砚发现了异常的拖拽痕迹。"
                }
              ],
              "dialogue": [
                {
                  "character_id": "char_shenyan",
                  "parenthetical": "压低声音",
                  "line": "这里昨晚一定出过事。",
                  "subtext": "他意识到案情并不简单"
                }
              ],
              "transition": "CUT_TO",
              "notes": {
                "emotion": "压抑",
                "pacing": "slow",
                "todo": "补充环境声"
              }
            }
            """);

        ScriptSceneResult result = sceneGenerateStep.execute(buildStoryBible(), buildScenePlan());

        assertThat(result.getId()).isEqualTo("sc01");
        assertThat(result.getSlugline()).isEqualTo("夜 外 旧城巷口");
        assertThat(result.getActions()).containsExactly("沈砚停在巷口，仔细观察墙角血迹。");
        assertThat(result.getBeats()).hasSize(1);
        assertThat(result.getDialogue()).hasSize(1);

        ScriptSceneBeat beat = result.getBeats().get(0);
        assertThat(beat.getId()).isEqualTo("beat01");

        ScriptSceneDialogue dialogue = result.getDialogue().get(0);
        assertThat(dialogue.getCharacterId()).isEqualTo("char_shenyan");
        assertThat(dialogue.getLine()).isEqualTo("这里昨晚一定出过事。");
        assertThat(result.getTransition()).isEqualTo("CUT_TO");

        verify(promptTemplateService).render(eq("scene-generate"), anyMap());
        verify(aiChatAdapter).chat(eq(SceneGenerateStep.SYSTEM_PROMPT), eq("请生成场景"));
    }

    private StoryBibleResult buildStoryBible() {
        StoryBibleResult result = new StoryBibleResult();
        result.setProjectId(1001L);

        StoryBibleCharacter character = new StoryBibleCharacter();
        character.setId("char_shenyan");
        character.setName("沈砚");
        character.setRole("protagonist");

        result.setCharacters(List.of(character));
        return result;
    }

    private ScriptOutlineScene buildScenePlan() {
        ScriptOutlineScene scene = new ScriptOutlineScene();
        scene.setId("sc01");
        scene.setSlugline("夜 外 旧城巷口");
        scene.setPurpose("建立悬疑氛围并引出主线线索");
        scene.setConflict("沈砚犹豫是否继续深入");
        scene.setSourceRefs(List.of("chapter:1"));
        scene.setCharacters(List.of("char_shenyan"));
        return scene;
    }
}
