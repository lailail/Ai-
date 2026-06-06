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

    @BeforeEach
    void setUp() {
        storyBibleBuildStep = new StoryBibleBuildStep(
            aiChatAdapter,
            promptTemplateService,
            new ObjectMapper()
        );
    }

    @Test
    void test_p3_c3_story_bible_build() {
        when(promptTemplateService.render(eq("story-bible-build"), anyMap())).thenReturn("请构建 Story Bible");
        when(aiChatAdapter.chat(eq(StoryBibleBuildStep.SYSTEM_PROMPT), eq("请构建 Story Bible"))).thenReturn("""
            {
              "characters": [
                {
                  "id": "char_shenyan",
                  "name": "沈砚",
                  "aliases": ["小沈"],
                  "role": "protagonist",
                  "traits": ["冷静", "敏锐"],
                  "goal": "查明旧案真相"
                }
              ],
              "relationships": [
                {
                  "from": "char_shenyan",
                  "to": "char_linwan",
                  "type": "ally",
                  "description": "两人因旧案暂时结盟"
                }
              ],
              "locations": [
                {
                  "id": "loc_qingshi",
                  "name": "青石巷",
                  "description": "旧城深处的狭窄巷道"
                }
              ],
              "timeline": [
                {
                  "id": "evt_001",
                  "order": 1,
                  "summary": "沈砚雨夜入城",
                  "source_refs": ["chapter:1"]
                }
              ],
              "conflicts": [
                {
                  "id": "conf_001",
                  "summary": "继续追查会触怒隐藏势力"
                }
              ],
              "foreshadowing": [
                {
                  "id": "foreshadow_001",
                  "setup": "铜牌反复出现",
                  "payoff_hint": "指向幕后身份",
                  "source_refs": ["chapter:2"]
                }
              ],
              "adaptation_strategy": ["前3章压缩为开篇一集", "保留旧城悬疑基调"]
            }
            """);

        StoryBibleResult result = storyBibleBuildStep.execute(1001L, buildGlobalContext());

        assertThat(result.getProjectId()).isEqualTo(1001L);
        assertThat(result.getCharacters()).hasSize(1);
        assertThat(result.getCharacters().get(0).getName()).isEqualTo("沈砚");
        assertThat(result.getRelationships()).hasSize(1);
        assertThat(result.getLocations()).hasSize(1);
        assertThat(result.getTimeline()).hasSize(1);
        assertThat(result.getTimeline().get(0).getSourceRefs()).containsExactly("chapter:1");
        assertThat(result.getConflicts()).hasSize(1);
        assertThat(result.getForeshadowing()).hasSize(1);
        assertThat(result.getAdaptationStrategy()).containsExactly("前3章压缩为开篇一集", "保留旧城悬疑基调");

        verify(promptTemplateService).render(eq("story-bible-build"), anyMap());
        verify(aiChatAdapter).chat(eq(StoryBibleBuildStep.SYSTEM_PROMPT), eq("请构建 Story Bible"));
    }

    private GlobalContextMergeResult buildGlobalContext() {
        GlobalContextMergeResult result = new GlobalContextMergeResult();
        result.setProjectId(1001L);
        result.setSummary("沈砚沿着铜牌线索追查旧案。");
        result.setCharacters(java.util.List.of("沈砚", "林晚", "老周"));
        result.setLocations(java.util.List.of("青石巷", "旧城码头"));
        result.setTimeline(java.util.List.of("沈砚入城", "发现铜牌"));
        result.setRelationships(java.util.List.of("沈砚与林晚形成临时同盟"));
        result.setConflicts(java.util.List.of("是否继续追查旧案"));
        result.setSourceContextRefs(java.util.List.of("chapter:1", "chapter:2", "chapter:3"));
        return result;
    }
}
