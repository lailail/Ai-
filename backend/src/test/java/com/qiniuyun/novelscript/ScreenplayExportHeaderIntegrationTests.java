package com.qiniuyun.novelscript;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
 * 验证正式剧本导出响应头可以兼容中文文件名，避免 Tomcat 因响应头编码报错。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScreenplayExportHeaderIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScriptVersionMapper scriptVersionMapper;

    @Autowired
    private YamlSnapshotMapper yamlSnapshotMapper;

    /**
     * 验证 Markdown 导出会同时返回 ASCII 回退文件名和 UTF-8 文件名扩展参数。
     *
     * @throws Exception 当请求执行失败时抛出
     */
    @Test
    void test_p4_export_header_should_support_utf8_filename() throws Exception {
        Long projectId = createProject("导出响应头测试项目");
        Long scriptVersionId = insertScriptVersion(
            projectId,
            3,
            "第3版长夜余烬-雨夜回城",
            "USER_EDITED",
            validYaml("导出响应头测试项目", "雨夜回城")
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

        mockMvc.perform(get("/api/projects/{projectId}/screenplays/{scriptVersionId}/export", projectId, scriptVersionId)
                .param("format", "md"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", containsString("filename=\"screenplay-v3.md\"")))
            .andExpect(header().string("Content-Disposition", containsString("filename*=UTF-8''")))
            .andExpect(header().string("Content-Disposition", containsString(".md")));
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
                          "description": "用于校验正式剧本导出响应头"
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
                    beats: []
                    dialogue:
                      - character_id: "char_ashu"
                        line: "这里比我记忆里更冷。"
                    transition: "CUT_TO"
            metadata:
              generated_at: "2026-06-07T10:00:00+08:00"
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
