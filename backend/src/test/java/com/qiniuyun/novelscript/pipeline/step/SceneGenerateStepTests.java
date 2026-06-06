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

    /**
     * 初始化场景生成步骤实例。
     */
    @BeforeEach
    void setUp() {
        sceneGenerateStep = new SceneGenerateStep(
            aiChatAdapter,
            promptTemplateService,
            new ObjectMapper()
        );
    }

    /**
     * 验证模型返回标准场景 JSON 时可以正确解析。
     */
    @Test
    void test_p3_c4_scene_generate() {
        when(promptTemplateService.render(eq("scene-generate"), anyMap())).thenReturn("generate scene");
        when(aiChatAdapter.chat(eq(SceneGenerateStep.SYSTEM_PROMPT), eq("generate scene"))).thenReturn("""
            {
              "id": "sc01",
              "slugline": "EXT. OLD TOWN ALLEY - NIGHT",
              "purpose": "Set up the mystery and reveal the first clue",
              "source_refs": ["chapter:1"],
              "characters": ["char_shenyan"],
              "actions": ["Shen Yan stops at the alley and studies the blood trace on the wall."],
              "beats": [
                {
                  "id": "beat01",
                  "action": "Shen Yan notices a drag mark on the ground."
                }
              ],
              "dialogue": [
                {
                  "character_id": "char_shenyan",
                  "parenthetical": "under his breath",
                  "line": "Something happened here last night.",
                  "subtext": "He knows this case is more dangerous than expected."
                }
              ],
              "transition": "CUT_TO",
              "notes": {
                "emotion": "tense",
                "pacing": "slow",
                "todo": "Add ambient sound details"
              }
            }
            """);

        ScriptSceneResult result = sceneGenerateStep.execute(buildStoryBible(), buildScenePlan());

        assertThat(result.getId()).isEqualTo("sc01");
        assertThat(result.getSlugline()).isEqualTo("EXT. OLD TOWN ALLEY - NIGHT");
        assertThat(result.getActions()).containsExactly("Shen Yan stops at the alley and studies the blood trace on the wall.");
        assertThat(result.getBeats()).hasSize(1);
        assertThat(result.getDialogue()).hasSize(1);

        ScriptSceneBeat beat = result.getBeats().get(0);
        assertThat(beat.getId()).isEqualTo("beat01");

        ScriptSceneDialogue dialogue = result.getDialogue().get(0);
        assertThat(dialogue.getCharacterId()).isEqualTo("char_shenyan");
        assertThat(dialogue.getLine()).isEqualTo("Something happened here last night.");
        assertThat(result.getTransition()).isEqualTo("CUT_TO");

        verify(promptTemplateService).render(eq("scene-generate"), anyMap());
        verify(aiChatAdapter).chat(eq(SceneGenerateStep.SYSTEM_PROMPT), eq("generate scene"));
    }

    /**
     * 验证 actions 返回对象数组时会被兼容转换为字符串数组。
     */
    @Test
    void test_p3_c4_scene_generate_actions_object_items() {
        when(promptTemplateService.render(eq("scene-generate"), anyMap())).thenReturn("generate scene");
        when(aiChatAdapter.chat(eq(SceneGenerateStep.SYSTEM_PROMPT), eq("generate scene"))).thenReturn("""
            {
              "id": "sc01",
              "slugline": "EXT. OLD TOWN ALLEY - NIGHT",
              "purpose": "Set up the mystery and reveal the first clue",
              "source_refs": ["chapter:1"],
              "characters": ["char_shenyan"],
              "actions": [
                {
                  "text": "Shen Yan checks the blood trace beside the old wall."
                },
                {
                  "action": "He compares the footprint depth with the drag mark."
                }
              ],
              "beats": [],
              "dialogue": [],
              "transition": "CUT_TO",
              "notes": {
                "emotion": "tense",
                "pacing": "slow",
                "todo": ""
              }
            }
            """);

        ScriptSceneResult result = sceneGenerateStep.execute(buildStoryBible(), buildScenePlan());

        assertThat(result.getActions()).containsExactly(
            "Shen Yan checks the blood trace beside the old wall.",
            "He compares the footprint depth with the drag mark."
        );
        assertThat(result.getCharacters()).containsExactly("char_shenyan");
        assertThat(result.getSourceRefs()).containsExactly("chapter:1");
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

        result.setCharacters(List.of(character));
        return result;
    }

    /**
     * 构造测试用场景规划结果。
     *
     * @return 场景规划结果
     */
    private ScriptOutlineScene buildScenePlan() {
        ScriptOutlineScene scene = new ScriptOutlineScene();
        scene.setId("sc01");
        scene.setSlugline("EXT. OLD TOWN ALLEY - NIGHT");
        scene.setPurpose("Set up the mystery and reveal the first clue");
        scene.setConflict("Shen Yan is unsure whether to continue the investigation");
        scene.setSourceRefs(List.of("chapter:1"));
        scene.setCharacters(List.of("char_shenyan"));
        return scene;
    }
}
