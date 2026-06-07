package com.qiniuyun.novelscript;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiniuyun.novelscript.domain.entity.ScriptVersion;
import com.qiniuyun.novelscript.domain.entity.YamlSnapshot;
import com.qiniuyun.novelscript.mapper.ScriptVersionMapper;
import com.qiniuyun.novelscript.mapper.YamlSnapshotMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 验证项目、章节与 YAML 工作区接口可用性的集成测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScriptVersionMapper scriptVersionMapper;

    @Autowired
    private YamlSnapshotMapper yamlSnapshotMapper;

    /**
     * 验证健康检查接口可正常返回。
     */
    @Test
    void shouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("UP"));
    }

    /**
     * 验证项目创建、章节保存与项目详情查询链路。
     */
    @Test
    void shouldCreateProjectAndSaveChapter() throws Exception {
        MvcResult projectResult = mockMvc.perform(
                post("/api/projects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "title": "长夜余烬",
                          "description": "用于联调的测试项目"
                        }
                        """)
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.title").value("长夜余烬"))
            .andReturn();

        Long projectId = extractProjectId(projectResult);

        mockMvc.perform(
                post("/api/projects/{projectId}/chapters", projectId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "chapterNo": 1,
                          "title": "第一章",
                          "content": "风从旧城墙背后吹来。"
                        }
                        """)
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.projectId").value(projectId))
            .andExpect(jsonPath("$.data.chapterNo").value(1));

        mockMvc.perform(get("/api/projects/{projectId}", projectId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(projectId))
            .andExpect(jsonPath("$.data.chapterCount").value(1));
    }

    /**
     * 验证项目列表与章节列表接口可返回最新录入数据。
     */
    @Test
    void test_pr4_1_list_projects_and_chapters() throws Exception {
        MvcResult projectResult = mockMvc.perform(
                post("/api/projects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "title": "前端联调项目",
                          "description": "用于验证项目列表和章节列表接口"
                        }
                        """)
            )
            .andExpect(status().isCreated())
            .andReturn();

        Long projectId = extractProjectId(projectResult);

        mockMvc.perform(
                post("/api/projects/{projectId}/chapters", projectId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "chapterNo": 1,
                          "title": "第一章",
                          "content": "夜色降临，旧码头只剩潮水声。"
                        }
                        """)
            )
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/projects"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].id").value(projectId))
            .andExpect(jsonPath("$.data[0].chapterCount").value(1));

        mockMvc.perform(get("/api/projects/{projectId}/chapters", projectId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].projectId").value(projectId))
            .andExpect(jsonPath("$.data[0].chapterNo").value(1))
            .andExpect(jsonPath("$.data[0].title").value("第一章"))
            .andExpect(jsonPath("$.data[0].content").value("夜色降临，旧码头只剩潮水声。"));
    }

    /**
     * 验证章节列表返回正文内容，并允许用户更新章节标题与正文。
     */
    @Test
    void test_pr4_4_update_chapter_content() throws Exception {
        MvcResult projectResult = mockMvc.perform(
                post("/api/projects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "title": "章节编辑测试项目",
                          "description": "用于验证章节查看与编辑"
                        }
                        """)
            )
            .andExpect(status().isCreated())
            .andReturn();

        Long projectId = extractProjectId(projectResult);

        MvcResult chapterResult = mockMvc.perform(
                post("/api/projects/{projectId}/chapters", projectId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "chapterNo": 1,
                          "title": "雨夜回城",
                          "content": "她在雨夜里重新回到了旧城。"
                        }
                        """)
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.content").value("她在雨夜里重新回到了旧城。"))
            .andReturn();

        Long chapterId = extractChapterId(chapterResult);

        mockMvc.perform(
                put("/api/projects/{projectId}/chapters/{chapterId}", projectId, chapterId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "title": "雨夜回城·修订版",
                          "content": "她在雨夜里重新回到了旧城，街灯把影子拉得很长。"
                        }
                        """)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.chapterNo").value(1))
            .andExpect(jsonPath("$.data.title").value("雨夜回城·修订版"))
            .andExpect(jsonPath("$.data.content").value("她在雨夜里重新回到了旧城，街灯把影子拉得很长。"));

        mockMvc.perform(get("/api/projects/{projectId}/chapters", projectId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].title").value("雨夜回城·修订版"))
            .andExpect(jsonPath("$.data[0].content").value("她在雨夜里重新回到了旧城，街灯把影子拉得很长。"));
    }

    /**
     * 验证剧本版本列表接口可返回按版本号倒序排列的结果。
     */
    @Test
    void test_pr4_3_list_script_versions() throws Exception {
        Long projectId = createProject("版本列表测试项目");
        insertScriptVersion(projectId, 1, "AI 初稿", "AI_GENERATED", validYaml("版本列表测试项目", "版本一"));
        Long latestVersionId = insertScriptVersion(projectId, 2, "人工修改稿", "USER_EDITED", validYaml("版本列表测试项目", "版本二"));

        mockMvc.perform(get("/api/projects/{projectId}/scripts", projectId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].scriptVersionId").value(latestVersionId))
            .andExpect(jsonPath("$.data[0].versionNo").value(2))
            .andExpect(jsonPath("$.data[0].sourceType").value("USER_EDITED"))
            .andExpect(jsonPath("$.data[0].latest").value(true))
            .andExpect(jsonPath("$.data[1].versionNo").value(1));
    }

    /**
     * 验证指定剧本版本详情接口可返回完整 YAML 内容。
     */
    @Test
    void test_pr4_3_get_script_version_detail() throws Exception {
        Long projectId = createProject("版本详情测试项目");
        Long scriptVersionId = insertScriptVersion(
            projectId,
            1,
            "第一版",
            "AI_GENERATED",
            validYaml("版本详情测试项目", "版本详情")
        );

        mockMvc.perform(get("/api/projects/{projectId}/scripts/{scriptVersionId}", projectId, scriptVersionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.projectId").value(projectId))
            .andExpect(jsonPath("$.data.scriptVersionId").value(scriptVersionId))
            .andExpect(jsonPath("$.data.title").value("第一版"))
            .andExpect(jsonPath("$.data.sourceType").value("AI_GENERATED"))
            .andExpect(jsonPath("$.data.yamlContent").isString());
    }

    /**
     * 验证 YAML 校验接口会返回结构化错误结果。
     */
    @Test
    void test_pr4_3_validate_yaml_content() throws Exception {
        Long projectId = createProject("YAML 校验测试项目");

        mockMvc.perform(
                post("/api/projects/{projectId}/scripts/validate", projectId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "yamlContent": "schema_version: \\"1.0\\"\\nproject:\\n  id: \\"demo\\"\\n"
                        }
                        """)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.projectId").value(projectId))
            .andExpect(jsonPath("$.data.valid").value(false))
            .andExpect(jsonPath("$.data.errors[0].path").isString());
    }

    /**
     * 验证保存 YAML 新版本接口会创建新的人工编辑版本。
     */
    @Test
    void test_pr4_3_save_new_script_version() throws Exception {
        Long projectId = createProject("保存版本测试项目");
        insertScriptVersion(projectId, 1, "AI 初稿", "AI_GENERATED", validYaml("保存版本测试项目", "初稿"));

        mockMvc.perform(
                post("/api/projects/{projectId}/scripts", projectId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "title": "作者精修版",
                          "yamlContent": "schema_version: \\"1.0\\"\\nproject:\\n  id: \\"project_1001\\"\\n  title: \\"保存版本测试项目\\"\\n  source_chapters: [1, 2, 3]\\n  adaptation_mode: \\"novel_to_screenplay\\"\\nstory_bible:\\n  characters:\\n    - id: \\"char_a\\"\\n      name: \\"阿朔\\"\\n  relationships: []\\n  locations: []\\n  timeline: []\\n  conflicts: []\\n  foreshadowing: []\\n  adaptation_strategy: []\\nepisodes:\\n  - id: \\"ep01\\"\\n    title: \\"作者精修版\\"\\n    premise: \\"保留核心冲突\\"\\n    source_refs: [\\"chapter:1\\"]\\n    scenes:\\n      - id: \\"sc01\\"\\n        slugline: \\"INT. 仓库 - NIGHT\\"\\n        purpose: \\"展示修订版内容\\"\\n        source_refs: [\\"chapter:1\\"]\\n        characters: [\\"char_a\\"]\\n        actions: [\\"阿朔推开仓库门。\\"]\\n        beats: []\\n        dialogue:\\n          - character_id: \\"char_a\\"\\n            line: \\"这里比我记忆里还冷。\\"\\n        transition: \\"CUT_TO\\"\\nmetadata:\\n  generated_at: \\"2026-06-06T14:10:00+08:00\\"\\n  generator: \\"deepseek-chat\\"\\n  notes: []\\n"
                        }
                        """)
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.versionNo").value(2))
            .andExpect(jsonPath("$.data.title").value("作者精修版"))
            .andExpect(jsonPath("$.data.sourceType").value("USER_EDITED"))
            .andExpect(jsonPath("$.data.validationStatus").value("PASSED"));

        mockMvc.perform(get("/api/projects/{projectId}/scripts", projectId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].versionNo").value(2))
            .andExpect(jsonPath("$.data[0].sourceType").value("USER_EDITED"));
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
                          "description": "用于 YAML 工作区测试"
                        }
                        """.formatted(title))
            )
            .andExpect(status().isCreated())
            .andReturn();
        return extractProjectId(projectResult);
    }

    /**
     * 为指定项目直接插入测试剧本版本和 YAML 快照。
     *
     * @param projectId 项目 ID
     * @param versionNo 版本号
     * @param title 版本标题
     * @param sourceType 版本来源
     * @param yamlContent YAML 原文
     * @return 新插入的剧本版本 ID
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
     * 构造一份可通过当前 Schema 校验的 YAML 文本。
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
                - id: "char_a"
                  name: "阿朔"
              relationships: []
              locations: []
              timeline: []
              conflicts: []
              foreshadowing: []
              adaptation_strategy: []
            episodes:
              - id: "ep01"
                title: "%s"
                premise: "保持主要人物动机"
                source_refs: ["chapter:1"]
                scenes:
                  - id: "sc01"
                    slugline: "INT. 仓库 - NIGHT"
                    purpose: "建立悬疑基调"
                    source_refs: ["chapter:1"]
                    characters: ["char_a"]
                    actions: ["阿朔走进昏暗仓库。"]
                    beats: []
                    dialogue:
                      - character_id: "char_a"
                        line: "我知道这里藏着答案。"
                    transition: "CUT_TO"
            metadata:
              generated_at: "2026-06-06T14:10:00+08:00"
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

    /**
     * 从创建章节响应中提取章节 ID。
     *
     * @param result 创建章节后的响应结果
     * @return 章节 ID
     * @throws Exception 当解析响应失败时抛出
     */
    private Long extractChapterId(MvcResult result) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.path("data").path("id").asLong();
    }
}
