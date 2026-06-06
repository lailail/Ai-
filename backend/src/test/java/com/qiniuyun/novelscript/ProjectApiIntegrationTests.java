package com.qiniuyun.novelscript;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 验证项目与章节最小接口可用性的集成测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
            .andExpect(jsonPath("$.data[0].title").value("第一章"));
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
