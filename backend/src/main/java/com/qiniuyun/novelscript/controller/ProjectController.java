package com.qiniuyun.novelscript.controller;

import com.qiniuyun.novelscript.common.response.ApiResponse;
import com.qiniuyun.novelscript.controller.request.ProjectCreateRequest;
import com.qiniuyun.novelscript.controller.request.ScriptValidateRequest;
import com.qiniuyun.novelscript.controller.request.ScriptVersionSaveRequest;
import com.qiniuyun.novelscript.controller.request.SourceChapterCreateRequest;
import com.qiniuyun.novelscript.controller.response.AdaptationJobResponse;
import com.qiniuyun.novelscript.controller.response.AdaptationScriptResponse;
import com.qiniuyun.novelscript.controller.response.ProjectResponse;
import com.qiniuyun.novelscript.controller.response.ScriptValidationResponse;
import com.qiniuyun.novelscript.controller.response.ScriptVersionSummaryResponse;
import com.qiniuyun.novelscript.controller.response.SourceChapterResponse;
import com.qiniuyun.novelscript.controller.response.StoryBibleResponse;
import com.qiniuyun.novelscript.service.AdaptationPipelineService;
import com.qiniuyun.novelscript.service.ContextSnapshotService;
import com.qiniuyun.novelscript.service.ProjectService;
import com.qiniuyun.novelscript.service.ScriptVersionService;
import com.qiniuyun.novelscript.service.SourceChapterService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
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
 * 改编项目、原始章节与剧本工作区相关的控制器。
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final SourceChapterService sourceChapterService;
    private final AdaptationPipelineService adaptationPipelineService;
    private final ContextSnapshotService contextSnapshotService;
    private final ScriptVersionService scriptVersionService;

    /**
     * 构造项目控制器。
     *
     * @param projectService 项目服务
     * @param sourceChapterService 章节服务
     * @param adaptationPipelineService 改编流水线服务
     * @param contextSnapshotService 上下文快照服务
     * @param scriptVersionService 剧本版本服务
     */
    public ProjectController(
        ProjectService projectService,
        SourceChapterService sourceChapterService,
        AdaptationPipelineService adaptationPipelineService,
        ContextSnapshotService contextSnapshotService,
        ScriptVersionService scriptVersionService
    ) {
        this.projectService = projectService;
        this.sourceChapterService = sourceChapterService;
        this.adaptationPipelineService = adaptationPipelineService;
        this.contextSnapshotService = contextSnapshotService;
        this.scriptVersionService = scriptVersionService;
    }

    /**
     * 创建新的改编项目。
     *
     * @param request 项目创建请求
     * @return 创建后的项目响应
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(@Valid @RequestBody ProjectCreateRequest request) {
        log.info("收到创建项目请求，标题：{}", request.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(projectService.createProject(request)));
    }

    /**
     * 查询当前所有改编项目摘要。
     *
     * @return 项目列表响应
     */
    @GetMapping
    public ApiResponse<List<ProjectResponse>> listProjects() {
        log.info("收到查询项目列表请求");
        return ApiResponse.success(projectService.listProjects());
    }

    /**
     * 查询指定改编项目详情。
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
     * 在指定项目下保存小说章节。
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
        log.info("收到保存章节请求，项目ID：{}，章节号：{}，标题：{}", projectId, request.getChapterNo(), request.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(sourceChapterService.createChapter(projectId, request)));
    }

    /**
     * 查询指定项目下已录入的章节列表。
     *
     * @param projectId 项目 ID
     * @return 章节列表响应
     */
    @GetMapping("/{projectId}/chapters")
    public ApiResponse<List<SourceChapterResponse>> listChapters(@PathVariable @Min(1) Long projectId) {
        log.info("收到查询章节列表请求，项目ID：{}", projectId);
        return ApiResponse.success(sourceChapterService.listChapters(projectId));
    }

    /**
     * 触发指定项目的改编任务。
     *
     * @param projectId 项目 ID
     * @return 任务进度响应
     */
    @PostMapping("/{projectId}/adaptations")
    public ResponseEntity<ApiResponse<AdaptationJobResponse>> generateScript(@PathVariable @Min(1) Long projectId) {
        log.info("收到改编生成请求，项目ID：{}", projectId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(ApiResponse.success(adaptationPipelineService.startAdaptation(projectId)));
    }

    /**
     * 查询指定项目当前最新的改编任务。
     *
     * @param projectId 项目 ID
     * @return 最新任务响应
     */
    @GetMapping("/{projectId}/adaptations/latest-job")
    public ApiResponse<AdaptationJobResponse> getLatestAdaptationJob(@PathVariable @Min(1) Long projectId) {
        log.info("收到查询最新改编任务请求，项目ID：{}", projectId);
        return ApiResponse.success(adaptationPipelineService.getLatestJob(projectId));
    }

    /**
     * 查询指定项目当前最新的 Story Bible 快照。
     *
     * @param projectId 项目 ID
     * @return 最新 Story Bible 响应
     */
    @GetMapping("/{projectId}/story-bible/latest")
    public ApiResponse<StoryBibleResponse> getLatestStoryBible(@PathVariable @Min(1) Long projectId) {
        log.info("收到查询最新 Story Bible 请求，项目ID：{}", projectId);
        return ApiResponse.success(contextSnapshotService.getLatestStoryBible(projectId));
    }

    /**
     * 查询指定项目当前最新的剧本版本。
     *
     * @param projectId 项目 ID
     * @return 最新剧本响应
     */
    @GetMapping("/{projectId}/scripts/latest")
    public ApiResponse<AdaptationScriptResponse> getLatestScript(@PathVariable @Min(1) Long projectId) {
        log.info("收到查询最新剧本请求，项目ID：{}", projectId);
        return ApiResponse.success(adaptationPipelineService.getLatestScript(projectId));
    }

    /**
     * 查询指定项目下的剧本版本列表。
     *
     * @param projectId 项目 ID
     * @return 按版本号倒序返回的版本摘要列表
     */
    @GetMapping("/{projectId}/scripts")
    public ApiResponse<List<ScriptVersionSummaryResponse>> listScriptVersions(@PathVariable @Min(1) Long projectId) {
        log.info("收到查询剧本版本列表请求，项目ID：{}", projectId);
        return ApiResponse.success(scriptVersionService.listScriptVersions(projectId));
    }

    /**
     * 查询指定项目下的剧本版本详情。
     *
     * @param projectId 项目 ID
     * @param scriptVersionId 剧本版本 ID
     * @return 指定版本详情
     */
    @GetMapping("/{projectId}/scripts/{scriptVersionId}")
    public ApiResponse<AdaptationScriptResponse> getScriptVersion(
        @PathVariable @Min(1) Long projectId,
        @PathVariable @Min(1) Long scriptVersionId
    ) {
        log.info("收到查询指定剧本版本请求，项目ID：{}，版本ID：{}", projectId, scriptVersionId);
        return ApiResponse.success(scriptVersionService.getScriptVersion(projectId, scriptVersionId));
    }

    /**
     * 对用户编辑后的 YAML 原文执行后端校验。
     *
     * @param projectId 项目 ID
     * @param request YAML 校验请求
     * @return 结构化校验结果
     */
    @PostMapping("/{projectId}/scripts/validate")
    public ApiResponse<ScriptValidationResponse> validateScript(
        @PathVariable @Min(1) Long projectId,
        @Valid @RequestBody ScriptValidateRequest request
    ) {
        log.info("收到剧本 YAML 校验请求，项目ID：{}", projectId);
        return ApiResponse.success(scriptVersionService.validateScript(projectId, request.getYamlContent()));
    }

    /**
     * 将用户编辑后的 YAML 保存为新剧本版本。
     *
     * @param projectId 项目 ID
     * @param request 剧本版本保存请求
     * @return 保存后的新版本详情
     */
    @PostMapping("/{projectId}/scripts")
    public ResponseEntity<ApiResponse<AdaptationScriptResponse>> saveScriptVersion(
        @PathVariable @Min(1) Long projectId,
        @Valid @RequestBody ScriptVersionSaveRequest request
    ) {
        log.info("收到保存剧本新版本请求，项目ID：{}", projectId);
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.success(scriptVersionService.saveScriptVersion(projectId, request.getTitle(), request.getYamlContent()))
        );
    }
}
