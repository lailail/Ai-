package com.qiniuyun.novelscript.controller;

import com.qiniuyun.novelscript.common.response.ApiResponse;
import com.qiniuyun.novelscript.controller.request.ProjectCreateRequest;
import com.qiniuyun.novelscript.controller.request.SourceChapterCreateRequest;
import com.qiniuyun.novelscript.controller.response.AdaptationScriptResponse;
import com.qiniuyun.novelscript.controller.response.ProjectResponse;
import com.qiniuyun.novelscript.controller.response.SourceChapterResponse;
import com.qiniuyun.novelscript.service.AdaptationPipelineService;
import com.qiniuyun.novelscript.service.ProjectService;
import com.qiniuyun.novelscript.service.SourceChapterService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 改编项目与小说章节的最小接口入口。
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final SourceChapterService sourceChapterService;
    private final AdaptationPipelineService adaptationPipelineService;

    public ProjectController(
        ProjectService projectService,
        SourceChapterService sourceChapterService,
        AdaptationPipelineService adaptationPipelineService
    ) {
        this.projectService = projectService;
        this.sourceChapterService = sourceChapterService;
        this.adaptationPipelineService = adaptationPipelineService;
    }

    /**
     * 创建新的改编项目。
     *
     * @param request 创建项目请求
     * @return 创建后的项目响应
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(@Valid @RequestBody ProjectCreateRequest request) {
        log.info("收到创建项目请求，标题：{}", request.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(projectService.createProject(request)));
    }

    /**
     * 查询指定改编项目。
     *
     * @param projectId 项目 ID
     * @return 项目详情响应
     */
    @GetMapping("/{projectId}")
    public ApiResponse<ProjectResponse> getProject(@PathVariable @Min(1) Long projectId) {
        log.info("收到查询项目请求，项目ID：{}", projectId);
        return ApiResponse.success(projectService.getProject(projectId));
    }

    /**
     * 在指定项目下保存章节。
     *
     * @param projectId 项目 ID
     * @param request 章节保存请求
     * @return 保存后的章节响应
     */
    @PostMapping("/{projectId}/chapters")
    public ResponseEntity<ApiResponse<SourceChapterResponse>> createChapter(
        @PathVariable @Min(1) Long projectId,
        @Valid @RequestBody SourceChapterCreateRequest request
    ) {
        log.info(
            "收到保存章节请求，项目ID：{}，章节号：{}，标题：{}",
            projectId,
            request.getChapterNo(),
            request.getTitle()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(sourceChapterService.createChapter(projectId, request)));
    }

    /**
     * 触发指定项目的最小改编生成流程。
     *
     * @param projectId 项目 ID
     * @return 生成后的最新剧本响应
     */
    @PostMapping("/{projectId}/adaptations")
    public ResponseEntity<ApiResponse<AdaptationScriptResponse>> generateScript(
        @PathVariable @Min(1) Long projectId
    ) {
        log.info("收到改编生成请求，项目ID：{}", projectId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(adaptationPipelineService.generateScript(projectId)));
    }

    /**
     * 查询指定项目的最新剧本版本。
     *
     * @param projectId 项目 ID
     * @return 最新剧本响应
     */
    @GetMapping("/{projectId}/scripts/latest")
    public ApiResponse<AdaptationScriptResponse> getLatestScript(@PathVariable @Min(1) Long projectId) {
        log.info("收到查询最新剧本请求，项目ID：{}", projectId);
        return ApiResponse.success(adaptationPipelineService.getLatestScript(projectId));
    }
}
