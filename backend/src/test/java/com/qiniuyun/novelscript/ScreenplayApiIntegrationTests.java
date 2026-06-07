package com.qiniuyun.novelscript;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiniuyun.novelscript.domain.entity.ScriptVersion;
import com.qiniuyun.novelscript.domain.entity.YamlSnapshot;
import com.qiniuyun.novelscript.mapper.ScriptVersionMapper;
import com.qiniuyun.novelscript.mapper.YamlSnapshotMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 验证正式剧本渲染、查询、回写和导出接口的集成测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScreenplayApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScriptVersionMapper scriptVersionMapper;

    @Autowired
    private YamlSnapshotMapper yamlSnapshotMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 验证后端可以根据指定 YAML 版本渲染正式剧本，并自动保存快照。
     */
    @Test
    void test_p4_screenplay_render_and_persist_snapshot() throws Exception {
        Long projectId = createProject("正式剧本渲染测试项目");
        Long scriptVersionId = insertScriptVersion(
            projectId,
            1,
            "第一版 YAML 初稿",
            "AI_GENERATED",
            validYaml("正式剧本渲染测试项目", "旧城夜行")
        );

        MvcResult renderResult = mockMvc.perform(
                post("/api/projects/{projectId}/screenplays/render", projectId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "scriptVersionId": %d
                        }
                        """.formatted(scriptVersionId))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.projectId").value(projectId))
            .andExpect(jsonPath("$.data.scriptVersionId").value(scriptVersionId))
            .andExpect(jsonPath("$.data.title").value("第一版 YAML 初稿"))
            .andExpect(jsonPath("$.data.markdownContent").value(containsString("旧城夜行")))
            .andExpect(jsonPath("$.data.markdownContent").value(containsString("INT. 旧仓库 - NIGHT")))
            .andExpect(jsonPath("$.data.markdownContent").value(containsString("阿述")))
            .andReturn();

        JsonNode renderNode = objectMapper.readTree(renderResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(renderNode.path("data").path("markdownContent").asText()).contains("这里比我记忆里更冷。");

        Integer snapshotCount = jdbcTemplate.queryForObject(
            "select count(*) from screenplay_snapshot where project_id = ? and script_version_id = ?",
            Integer.class,
            projectId,
            scriptVersionId
        );
        assertThat(snapshotCount).isEqualTo(1);
    }

    /**
     * 验证后端可以查询最新正式剧本，并支持导出 Markdown 和 TXT。
     */
    @Test
    void test_p4_screenplay_query_and_export() throws Exception {
        Long projectId = createProject("正式剧本查询导出测试项目");
        Long scriptVersionId = insertScriptVersion(
            projectId,
            2,
            "作者修订版",
            "USER_EDITED",
            validYaml("正式剧本查询导出测试项目", "河岸对峙")
        );

        mockMvc.perform(
                post("/api/projects/{projectId}/screenplays/render", projectId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "scriptVersionId": %d
                        }
                        """.formatted(scriptVersionId))
            )
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/projects/{projectId}/screenplays/latest", projectId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.scriptVersionId").value(scriptVersionId))
            .andExpect(jsonPath("$.data.markdownContent").value(containsString("河岸对峙")));

        mockMvc.perform(get("/api/projects/{projectId}/screenplays/{scriptVersionId}", projectId, scriptVersionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.sourceType").value("USER_EDITED"));

        mockMvc.perform(get("/api/projects/{projectId}/screenplays/{scriptVersionId}/export", projectId, scriptVersionId)
                .param("format", "md"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", containsString(".md")))
            .andExpect(header().string("Content-Type", containsString("text/markdown")));

        mockMvc.perform(get("/api/projects/{projectId}/screenplays/{scriptVersionId}/export", projectId, scriptVersionId)
                .param("format", "txt"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", containsString(".txt")))
            .andExpect(header().string("Content-Type", containsString("text/plain")));
    }

    /**
     * 验证正式剧本编辑后可以回写 YAML，并生成新的剧本版本和正式剧本快照。
     */
    @Test
    void test_p4_screenplay_sync_yaml_and_save_new_version() throws Exception {
        Long projectId = createProject("正式剧本回写测试项目");
        Long scriptVersionId = insertScriptVersion(
            projectId,
            1,
            "第一版 YAML 初稿",
            "AI_GENERATED",
            validYaml("正式剧本回写测试项目", "河岸归来")
        );

        mockMvc.perform(
                post("/api/projects/{projectId}/screenplays/render", projectId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "scriptVersionId": %d
                        }
                        """.formatted(scriptVersionId))
            )
            .andExpect(status().isOk());

        mockMvc.perform(
                post("/api/projects/{projectId}/screenplays/sync-yaml", projectId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(buildSyncYamlRequest(scriptVersionId)))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.versionNo").value(2))
            .andExpect(jsonPath("$.data.title").value("作者修订版剧本"))
            .andExpect(jsonPath("$.data.yamlContent").value(containsString("title: \"河岸重访\"")))
            .andExpect(jsonPath("$.data.yamlContent").value(containsString("slugline: \"EXT. 旧城河岸 - DAWN\"")))
            .andExpect(jsonPath("$.data.yamlContent").value(containsString("这一次，我不会再转身。")));

        mockMvc.perform(get("/api/projects/{projectId}/scripts", projectId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].versionNo").value(2))
            .andExpect(jsonPath("$.data[0].title").value("作者修订版剧本"));

        mockMvc.perform(get("/api/projects/{projectId}/screenplays/latest", projectId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.markdownContent").value(containsString("EXT. 旧城河岸 - DAWN")))
            .andExpect(jsonPath("$.data.markdownContent").value(containsString("这一次，我不会再转身。")));
    }

    /**
     * 构造正式剧本回写请求体。
     *
     * @param scriptVersionId 原始剧本版本 ID
     * @return 可直接序列化的请求对象
     */
    private Map<String, Object> buildSyncYamlRequest(Long scriptVersionId) {
        return Map.of(
            "scriptVersionId", scriptVersionId,
            "title", "作者修订版剧本",
            "markdownContent", """
                # 正式剧本回写测试项目 正式剧本

                ## 第1集：河岸重访

                > 剧集前提：保留主人公的迟疑与试探

                ### 场 1
                EXT. 旧城河岸 - DAWN

                目的：建立悬疑氛围

                阿述站在河岸边，捏紧那封失而复得的信。

                阿述
                这一次，我不会再转身。

                CUT_TO
                """.stripIndent()
        );
    }

    /**
     * 通过项目创建接口创建测试项目。
     *
     * @param title 项目标题
     * @return 新建项目 ID
     * @throws Exception 当接口调用失败时抛出
     */
    private Long createProject(String title) throws Exception {
        MvcResult projectResult = mockMvc.perform(
                post("/api/projects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "title": "%s",
                          "description": "用于正式剧本链路联调"
                        }
                        """.formatted(title))
            )
            .andExpect(status().isCreated())
            .andReturn();
        return extractProjectId(projectResult);
    }

    /**
     * 为指定项目插入一份 YAML 剧本版本和快照。
     *
     * @param projectId 项目 ID
     * @param versionNo 版本号
     * @param title 版本标题
     * @param sourceType 版本来源
     * @param yamlContent YAML 内容
     * @return 剧本版本 ID
     */
    private Long insertScriptVersion(
        Long projectId,
        Integer versionNo,
        String title,
        String sourceType,
        String yamlContent
    ) {
        LocalDateTime now = LocalDateTime.now();

        ScriptVersion scriptVersion = new ScriptVersion();
        scriptVersion.setProjectId(projectId);
        scriptVersion.setVersionNo(versionNo);
        scriptVersion.setTitle(title);
        scriptVersion.setSourceType(sourceType);
        scriptVersion.setCreatedAt(now);
        scriptVersion.setUpdatedAt(now);
        scriptVersionMapper.insert(scriptVersion);

        YamlSnapshot yamlSnapshot = new YamlSnapshot();
        yamlSnapshot.setProjectId(projectId);
        yamlSnapshot.setScriptVersionId(scriptVersion.getId());
        yamlSnapshot.setSchemaVersion("1.0");
        yamlSnapshot.setYamlContent(yamlContent);
        yamlSnapshot.setValidationStatus("PASSED");
        yamlSnapshot.setValidationErrors("[]");
        yamlSnapshot.setCreatedAt(now);
        yamlSnapshot.setUpdatedAt(now);
        yamlSnapshotMapper.insert(yamlSnapshot);
        return scriptVersion.getId();
    }

    /**
     * 构造一份可以通过当前 Schema 校验的 YAML 文本。
     *
     * @param projectTitle 项目标题
     * @param episodeTitle 剧集标题
     * @return 合法的 YAML 文本
     */
    private String validYaml(String projectTitle, String episodeTitle) {
        return """
            schema_version: "1.0"
            project:
              id: "project_1001"
              title: "%s"
              source_chapters: [1, 2, 3]
              adaptation_mode: "novel_to_screenplay"
            story_bible:
              characters:
                - id: "char_ashu"
                  name: "阿述"
              relationships: []
              locations: []
              timeline: []
              conflicts: []
              foreshadowing: []
              adaptation_strategy: []
            episodes:
              - id: "ep01"
                title: "%s"
                premise: "保留主人公的迟疑与试探"
                source_refs: ["chapter:1"]
                scenes:
                  - id: "sc01"
                    slugline: "INT. 旧仓库 - NIGHT"
                    purpose: "建立悬疑氛围"
                    source_refs: ["chapter:1"]
                    characters: ["char_ashu"]
                    actions:
                      - "阿述推开旧仓库的铁门，冷风立刻灌了进来。"
                      - "她沿着昏暗通道缓慢前进，鞋底碾过碎玻璃。"
                    beats: []
                    dialogue:
                      - character_id: "char_ashu"
                        line: "这里比我记忆里更冷。"
                    transition: "CUT_TO"
            metadata:
              generated_at: "2026-06-06T18:10:00+08:00"
              generator: "deepseek-chat"
              notes: []
            """.formatted(projectTitle, episodeTitle);
    }

    /**
     * 从创建项目响应中提取项目 ID。
     *
     * @param result 创建项目后的响应结果
     * @return 项目 ID
     * @throws Exception 当解析响应失败时抛出
     */
    private Long extractProjectId(MvcResult result) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.path("data").path("id").asLong();
    }
}
