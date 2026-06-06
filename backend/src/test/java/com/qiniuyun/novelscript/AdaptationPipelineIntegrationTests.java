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
 * 验证改编主链路与任务查询接口的最小集成行为。
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

    /**
     * 验证开始改编后可以查询到最新任务进度，并在完成后查询到最新剧本。
     *
     * @throws Exception 接口调用失败时抛出
     */
    @Test
    void test_pr4_tabs_workspace_job_progress() throws Exception {
        mockAiResponses();
        Long projectId = createProjectAndChapters();

        mockMvc.perform(post("/api/projects/{projectId}/adaptations", projectId))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.projectId").value(projectId))
            .andExpect(jsonPath("$.data.jobId").isNumber())
            .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
            .andExpect(jsonPath("$.data.currentStage").value("COMPLETED"))
            .andExpect(jsonPath("$.data.progressPercent").value(100));

        mockMvc.perform(get("/api/projects/{projectId}/adaptations/latest-job", projectId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.projectId").value(projectId))
            .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
            .andExpect(jsonPath("$.data.currentStage").value("COMPLETED"))
            .andExpect(jsonPath("$.data.progressPercent").value(100))
            .andExpect(jsonPath("$.data.stageCount").value(9));

        mockMvc.perform(get("/api/projects/{projectId}/scripts/latest", projectId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.projectId").value(projectId))
            .andExpect(jsonPath("$.data.versionNo").value(1))
            .andExpect(jsonPath("$.data.schemaVersion").value("1.0"))
            .andExpect(jsonPath("$.data.yamlContent").isNotEmpty());
    }

    /**
     * 验证生成完成后可以查询到最新 Story Bible 快照。
     *
     * @throws Exception 接口调用失败时抛出
     */
    @Test
    void test_pr4_2_get_latest_story_bible() throws Exception {
        mockAiResponses();
        Long projectId = createProjectAndChapters();

        mockMvc.perform(post("/api/projects/{projectId}/adaptations", projectId))
            .andExpect(status().isAccepted());

        mockMvc.perform(get("/api/projects/{projectId}/story-bible/latest", projectId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.projectId").value(projectId))
            .andExpect(jsonPath("$.data.versionNo").value(1))
            .andExpect(jsonPath("$.data.storyBibleId").isNumber())
            .andExpect(jsonPath("$.data.characters[0].id").value("char_shenyan"))
            .andExpect(jsonPath("$.data.locations[0].id").value("loc_old_street"))
            .andExpect(jsonPath("$.data.conflicts[0].id").value("conf_001"))
            .andExpect(jsonPath("$.data.adaptationStrategy[0]").value("Compress the first three chapters into episode one"));
    }

    /**
     * 模拟改编流水线各阶段所需的 AI 返回结果。
     */
    private void mockAiResponses() {
        when(aiChatAdapter.chat(anyString(), anyString())).thenReturn(
            """
                {
                  "summary": "Shen Yan enters the old town and notices unusual traces.",
                  "characters": ["Shen Yan"],
                  "locations": ["Old Town Alley"],
                  "events": ["Shen Yan enters town"],
                  "conflicts": ["Should he keep investigating"],
                  "emotion_changes": ["Alert"],
                  "foreshadowing": ["Blood near the wall"],
                  "key_dialogues": ["Shen Yan: Something is wrong here."],
                  "source_refs": ["chapter:1"]
                }
                """,
            """
                {
                  "summary": "Lao Zhou brings the bronze token and deepens the clue.",
                  "characters": ["Lao Zhou"],
                  "locations": ["Tea House"],
                  "events": ["The bronze token appears"],
                  "conflicts": ["The clue may be misleading"],
                  "emotion_changes": ["Suspicious"],
                  "foreshadowing": ["Marks on the token"],
                  "key_dialogues": ["Lao Zhou: This is not an ordinary item."],
                  "source_refs": ["chapter:2"]
                }
                """,
            """
                {
                  "summary": "Lin Wan reveals the old case background and clarifies the main line.",
                  "characters": ["Lin Wan"],
                  "locations": ["Archive Room"],
                  "events": ["Old case information is revealed"],
                  "conflicts": ["Whether the truth should be made public"],
                  "emotion_changes": ["Heavy"],
                  "foreshadowing": ["Missing name list"],
                  "key_dialogues": ["Lin Wan: The past never really ended."],
                  "source_refs": ["chapter:3"]
                }
                """,
            """
                {
                  "summary": "Shen Yan follows the old case clues and gets closer to the truth.",
                  "characters": ["Shen Yan", "Lao Zhou", "Lin Wan"],
                  "locations": ["Old Town Alley", "Tea House", "Archive Room"],
                  "timeline": ["Shen Yan enters town", "The bronze token appears", "Old case information is revealed"],
                  "relationships": ["Shen Yan and Lin Wan form a temporary alliance"],
                  "conflicts": ["Should he keep investigating", "The cost of revealing the truth"],
                  "source_context_refs": ["chapter:1", "chapter:2", "chapter:3"]
                }
                """,
            """
                {
                  "characters": [
                    {
                      "id": "char_shenyan",
                      "name": "Shen Yan",
                      "aliases": [],
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
                      "id": "loc_old_street",
                      "name": "Old Town Alley",
                      "description": "A narrow alley at the entrance of the old town"
                    }
                  ],
                  "timeline": [
                    {
                      "id": "evt_001",
                      "order": 1,
                      "summary": "Shen Yan enters town",
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
                      "setup": "Blood near the wall",
                      "payoff_hint": "Points back to the old case scene",
                      "source_refs": ["chapter:1"]
                    }
                  ],
                  "adaptation_strategy": ["Compress the first three chapters into episode one"]
                }
                """,
            """
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
                """,
            """
                {
                  "id": "sc01",
                  "slugline": "EXT. OLD TOWN ALLEY - NIGHT",
                  "purpose": "Set the suspense tone and introduce the main clue",
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
                      "subtext": "He realizes the old case is more dangerous than expected."
                    }
                  ],
                  "transition": "CUT_TO",
                  "notes": {
                    "emotion": "tense",
                    "pacing": "slow",
                    "todo": "Add ambient sound details"
                  }
                }
                """
        );
    }

    /**
     * 创建测试项目并录入三章原始小说内容。
     *
     * @return 创建后的项目 ID
     * @throws Exception 接口调用失败时抛出
     */
    private Long createProjectAndChapters() throws Exception {
        MvcResult projectResult = mockMvc.perform(
                post("/api/projects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "title": "Long Night Ember",
                          "description": "Used to verify the adaptation pipeline."
                        }
                        """)
            )
            .andExpect(status().isCreated())
            .andReturn();

        Long projectId = extractProjectId(projectResult);

        createChapter(projectId, 1, "Chapter 1 Arrival", "Shen Yan returns to the old town and finds blood near the wall.");
        createChapter(projectId, 2, "Chapter 2 Token", "Lao Zhou brings a bronze token marked with the old case number.");
        createChapter(projectId, 3, "Chapter 3 Old Case", "Lin Wan reveals the old case background and warns Shen Yan.");
        return projectId;
    }

    /**
     * 向项目下创建单章原始小说内容。
     *
     * @param projectId 项目 ID
     * @param chapterNo 章节号
     * @param title 标题
     * @param content 正文
     * @throws Exception 接口调用失败时抛出
     */
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

    /**
     * 从创建项目接口响应中提取项目 ID。
     *
     * @param result 创建项目接口响应
     * @return 项目 ID
     * @throws Exception JSON 解析失败时抛出
     */
    private Long extractProjectId(MvcResult result) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.path("data").path("id").asLong();
    }
}
