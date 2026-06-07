package com.qiniuyun.novelscript.pipeline.step;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiniuyun.novelscript.ai.adapter.AiChatAdapter;
import com.qiniuyun.novelscript.ai.prompt.PromptTemplateService;
import com.qiniuyun.novelscript.pipeline.model.GlobalContextMergeResult;
import com.qiniuyun.novelscript.pipeline.model.StoryBibleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 验证 Story Bible 构建步骤的结构化解析行为。
 */
@ExtendWith(MockitoExtension.class)
class StoryBibleBuildStepTests {

    @Mock
    private AiChatAdapter aiChatAdapter;

    @Mock
    private PromptTemplateService promptTemplateService;

    private StoryBibleBuildStep storyBibleBuildStep;

    /**
     * 初始化 Story Bible 构建步骤实例。
     */
    @BeforeEach
    void setUp() {
        storyBibleBuildStep = new StoryBibleBuildStep(
            aiChatAdapter,
            promptTemplateService,
            new ObjectMapper()
        );
    }

    /**
     * 验证全局上下文可以被正确解析为 Story Bible 结构。
     */
    @Test
    void test_p3_c3_story_bible_build() {
        when(promptTemplateService.render(eq("story-bible-build"), anyMap())).thenReturn("build story bible");
        when(aiChatAdapter.chat(eq(StoryBibleBuildStep.SYSTEM_PROMPT), eq("build story bible"))).thenReturn("""
            {
              "characters": [
                {
                  "id": "char_shenyan",
                  "name": "Shen Yan",
                  "aliases": ["Xiao Shen"],
                  "role": "protagonist",
                  "traits": ["calm", "sharp"],
                  "goal": "Find the truth of the old case"
                }
              ],
              "relationships": [
                {
                  "from": "char_shenyan",
                  "to": "char_linwan",
                  "type": "ally",
                  "description": "They temporarily work together because of the old case"
                }
              ],
              "locations": [
                {
                  "id": "loc_stone_alley",
                  "name": "Stone Alley",
                  "description": "A narrow alley deep inside the old town"
                }
              ],
              "timeline": [
                {
                  "id": "evt_001",
                  "order": 1,
                  "summary": "Shen Yan enters town on a rainy night",
                  "source_refs": ["chapter:1"]
                }
              ],
              "conflicts": [
                {
                  "id": "conf_001",
                  "summary": "Further investigation may anger hidden forces"
                }
              ],
              "foreshadowing": [
                {
                  "id": "foreshadow_001",
                  "setup": "The bronze token keeps reappearing",
                  "payoff_hint": "It points to the person behind the scenes",
                  "source_refs": ["chapter:2"]
                }
              ],
              "adaptation_strategy": ["Compress the first three chapters into episode one", "Keep the old town suspense tone"]
            }
            """);

        StoryBibleResult result = storyBibleBuildStep.execute(1001L, buildGlobalContext());

        assertThat(result.getProjectId()).isEqualTo(1001L);
        assertThat(result.getCharacters()).hasSize(1);
        assertThat(result.getCharacters().get(0).getName()).isEqualTo("Shen Yan");
        assertThat(result.getRelationships()).hasSize(1);
        assertThat(result.getLocations()).hasSize(1);
        assertThat(result.getTimeline()).hasSize(1);
        assertThat(result.getTimeline().get(0).getSourceRefs()).containsExactly("chapter:1");
        assertThat(result.getConflicts()).hasSize(1);
        assertThat(result.getForeshadowing()).hasSize(1);
        assertThat(result.getAdaptationStrategy()).containsExactly(
            "Compress the first three chapters into episode one",
            "Keep the old town suspense tone"
        );

        verify(promptTemplateService).render(eq("story-bible-build"), anyMap());
        verify(aiChatAdapter).chat(eq(StoryBibleBuildStep.SYSTEM_PROMPT), eq("build story bible"));
    }

    /**
     * 验证模型把列表字段返回成单个字符串时，步骤仍能兼容解析。
     */
    @Test
    void test_p3_c3_story_bible_build_accepts_string_traits() {
        when(promptTemplateService.render(eq("story-bible-build"), anyMap())).thenReturn("build story bible");
        when(aiChatAdapter.chat(eq(StoryBibleBuildStep.SYSTEM_PROMPT), eq("build story bible"))).thenReturn("""
            {
              "characters": [
                {
                  "id": "char_jiangning",
                  "name": "江宁",
                  "aliases": "阿宁",
                  "role": "protagonist",
                  "traits": "执拗、敏锐、三年前曾怯懦如今坚定",
                  "goal": "查清旧案真相"
                }
              ],
              "relationships": [
                {
                  "from": "char_jiangning",
                  "to": "char_qinzhou",
                  "type": "ally",
                  "description": "两人因追查旧案而暂时合作"
                }
              ],
              "locations": [
                {
                  "id": "loc_old_city",
                  "name": "旧城巷口",
                  "description": "雨夜中灯火昏黄的街巷"
                }
              ],
              "timeline": [
                {
                  "id": "evt_001",
                  "order": 1,
                  "summary": "江宁回到故城",
                  "source_refs": "chapter:1"
                }
              ],
              "conflicts": [
                {
                  "id": "conf_001",
                  "summary": "继续追查可能牵出更危险的势力"
                }
              ],
              "foreshadowing": [
                {
                  "id": "foreshadow_001",
                  "setup": "铜牌再次出现",
                  "payoff_hint": "它和幕后之人有关",
                  "source_refs": "chapter:2"
                }
              ],
              "adaptation_strategy": "强化雨夜悬疑氛围，压缩成第一集"
            }
            """);

        StoryBibleResult result = storyBibleBuildStep.execute(1002L, buildGlobalContext());

        assertThat(result.getProjectId()).isEqualTo(1002L);
        assertThat(result.getCharacters()).hasSize(1);
        assertThat(result.getCharacters().get(0).getAliases()).containsExactly("阿宁");
        assertThat(result.getCharacters().get(0).getTraits()).containsExactly("执拗", "敏锐", "三年前曾怯懦如今坚定");
        assertThat(result.getTimeline().get(0).getSourceRefs()).containsExactly("chapter:1");
        assertThat(result.getForeshadowing().get(0).getSourceRefs()).containsExactly("chapter:2");
        assertThat(result.getAdaptationStrategy()).containsExactly("强化雨夜悬疑氛围", "压缩成第一集");
    }

    /**
     * 构造测试用全局上下文结果。
     *
     * @return 全局上下文结果
     */
    private GlobalContextMergeResult buildGlobalContext() {
        GlobalContextMergeResult result = new GlobalContextMergeResult();
        result.setProjectId(1001L);
        result.setSummary("Shen Yan follows the token clue to investigate the old case.");
        result.setCharacters(java.util.List.of("Shen Yan", "Lin Wan", "Lao Zhou"));
        result.setLocations(java.util.List.of("Stone Alley", "Old Wharf"));
        result.setTimeline(java.util.List.of("Shen Yan enters town", "The bronze token appears"));
        result.setRelationships(java.util.List.of("Shen Yan and Lin Wan form a temporary alliance"));
        result.setConflicts(java.util.List.of("Should he continue the old case investigation"));
        result.setSourceContextRefs(java.util.List.of("chapter:1", "chapter:2", "chapter:3"));
        return result;
    }
}
