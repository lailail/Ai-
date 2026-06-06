package com.qiniuyun.novelscript;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiniuyun.novelscript.ai.adapter.AiChatAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 验证改编主链路的最小集成行为。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdaptationPipelineIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiChatAdapter aiChatAdapter;

    @Test
    void test_p3_c5_generate_and_get_latest_script() throws Exception {
        mockAiResponses();
        Long projectId = createProjectAndChapters();

        mockMvc.perform(post("/api/projects/{projectId}/adaptations", projectId))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.projectId").value(projectId))
            .andExpect(jsonPath("$.data.versionNo").value(1))
            .andExpect(jsonPath("$.data.validationStatus").value("PASSED"))
            .andExpect(jsonPath("$.data.yamlContent").isNotEmpty())
            .andExpect(jsonPath("$.data.jobStatus").value("SUCCEEDED"));

        mockMvc.perform(get("/api/projects/{projectId}/scripts/latest", projectId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.projectId").value(projectId))
            .andExpect(jsonPath("$.data.versionNo").value(1))
            .andExpect(jsonPath("$.data.schemaVersion").value("1.0"))
            .andExpect(jsonPath("$.data.yamlContent").isNotEmpty());
    }

    private void mockAiResponses() {
        when(aiChatAdapter.chat(anyString(), anyString())).thenReturn(
            """
                {
                  "summary": "沈砚进入旧城并发现异常线索。",
                  "characters": ["沈砚"],
                  "locations": ["旧城巷口"],
                  "events": ["沈砚入城"],
                  "conflicts": ["是否继续追查"],
                  "emotion_changes": ["警惕"],
                  "foreshadowing": ["墙角血迹"],
                  "key_dialogues": ["沈砚：这里不对劲。"],
                  "source_refs": ["chapter:1"]
                }
                """,
            """
                {
                  "summary": "老周带来铜牌，线索加深。",
                  "characters": ["老周"],
                  "locations": ["旧城茶馆"],
                  "events": ["铜牌出现"],
                  "conflicts": ["线索真假难辨"],
                  "emotion_changes": ["怀疑"],
                  "foreshadowing": ["铜牌刻痕"],
                  "key_dialogues": ["老周：这东西不是普通物件。"],
                  "source_refs": ["chapter:2"]
                }
                """,
            """
                {
                  "summary": "林晚说出旧案背景，主线明确。",
                  "characters": ["林晚"],
                  "locations": ["旧城档案室"],
                  "events": ["旧案信息公开"],
                  "conflicts": ["是否公开真相"],
                  "emotion_changes": ["压抑"],
                  "foreshadowing": ["失踪名单"],
                  "key_dialogues": ["林晚：当年的事没有结束。"],
                  "source_refs": ["chapter:3"]
                }
                """,
            """
                {
                  "summary": "沈砚沿着旧案线索逐步逼近真相。",
                  "characters": ["沈砚", "老周", "林晚"],
                  "locations": ["旧城巷口", "旧城茶馆", "旧城档案室"],
                  "timeline": ["沈砚入城", "铜牌出现", "旧案信息公开"],
                  "relationships": ["沈砚与林晚形成临时同盟"],
                  "conflicts": ["是否继续追查", "真相公开后的代价"],
                  "source_context_refs": ["chapter:1", "chapter:2", "chapter:3"]
                }
                """,
            """
                {
                  "characters": [
                    {
                      "id": "char_shenyan",
                      "name": "沈砚",
                      "aliases": [],
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
                      "id": "loc_old_street",
                      "name": "旧城巷口",
                      "description": "旧城入口处的狭窄街巷"
                    }
                  ],
                  "timeline": [
                    {
                      "id": "evt_001",
                      "order": 1,
                      "summary": "沈砚入城",
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
                      "setup": "墙角血迹",
                      "payoff_hint": "指向旧案现场",
                      "source_refs": ["chapter:1"]
                    }
                  ],
                  "adaptation_strategy": ["前三章压缩为开篇一集"]
                }
                """,
            """
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
                """,
            """
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
                """
        );
    }

    private Long createProjectAndChapters() throws Exception {
        MvcResult projectResult = mockMvc.perform(
                post("/api/projects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "title": "长夜余烬",
                          "description": "用于验证改编主链路"
                        }
                        """)
            )
            .andExpect(status().isCreated())
            .andReturn();

        Long projectId = extractProjectId(projectResult);

        createChapter(projectId, 1, "第一章 夜雨入城", "第一章 夜雨入城\n沈砚第一次走进旧城巷口。");
        createChapter(projectId, 2, "第二章 铜牌", "第二章 铜牌\n老周带来了一块旧铜牌。");
        createChapter(projectId, 3, "第三章 旧案", "第三章 旧案\n林晚说出了当年旧案的细节。");
        return projectId;
    }

    private void createChapter(Long projectId, int chapterNo, String title, String content) throws Exception {
        mockMvc.perform(
                post("/api/projects/{projectId}/chapters", projectId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "chapterNo": %d,
                          "title": "%s",
                          "content": "%s"
                        }
                        """.formatted(chapterNo, title, content.replace("\n", "\\n")))
            )
            .andExpect(status().isCreated());
    }

    private Long extractProjectId(MvcResult result) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.path("data").path("id").asLong();
    }
}
