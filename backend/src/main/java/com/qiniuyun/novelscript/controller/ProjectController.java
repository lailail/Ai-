package com.qiniuyun.novelscript.controller;

import com.qiniuyun.novelscript.common.response.ApiResponse;
import com.qiniuyun.novelscript.controller.request.ProjectCreateRequest;
import com.qiniuyun.novelscript.controller.request.ScreenplayRenderRequest;
import com.qiniuyun.novelscript.controller.request.ScreenplaySaveRequest;
import com.qiniuyun.novelscript.controller.request.ScreenplaySyncYamlRequest;
import com.qiniuyun.novelscript.controller.request.ScriptValidateRequest;
import com.qiniuyun.novelscript.controller.request.ScriptVersionSaveRequest;
import com.qiniuyun.novelscript.controller.request.SourceChapterCreateRequest;
import com.qiniuyun.novelscript.controller.response.AdaptationJobResponse;
import com.qiniuyun.novelscript.controller.response.AdaptationScriptResponse;
import com.qiniuyun.novelscript.controller.response.ProjectResponse;
import com.qiniuyun.novelscript.controller.response.ScreenplayResponse;
import com.qiniuyun.novelscript.controller.response.ScriptValidationResponse;
import com.qiniuyun.novelscript.controller.response.ScriptVersionSummaryResponse;
import com.qiniuyun.novelscript.controller.response.SourceChapterResponse;
import com.qiniuyun.novelscript.controller.response.StoryBibleResponse;
import com.qiniuyun.novelscript.service.AdaptationPipelineService;
import com.qiniuyun.novelscript.service.ContextSnapshotService;
import com.qiniuyun.novelscript.service.ProjectService;
import com.qiniuyun.novelscript.service.ScreenplayService;
import com.qiniuyun.novelscript.service.ScriptVersionService;
import com.qiniuyun.novelscript.service.SourceChapterService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 负责改编项目、章节、YAML 工作区和正式剧本相关接口的控制器。
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
    private final ScreenplayService screenplayService;

    /**
     * 构造项目控制器。
     *
     * @param projectService 项目服务
     * @param sourceChapterService 章节服务
     * @param adaptationPipelineService 改编流水线服务
     * @param contextSnapshotService 上下文快照服务
     * @param scriptVersionService 剧本版本服务
     * @param screenplayService 正式剧本服务
     */
    public ProjectController(
        ProjectService projectService,
        SourceChapterService sourceChapterService,
        AdaptationPipelineService adaptationPipelineService,
        ContextSnapshotService contextSnapshotService,
        ScriptVersionService scriptVersionService,
        ScreenplayService screenplayService
    ) {
        this.projectService = projectService;
        this.sourceChapterService = sourceChapterService;
        this.adaptationPipelineService = adaptationPipelineService;
        this.contextSnapshotService = contextSnapshotService;
        this.scriptVersionService = scriptVersionService;
        this.screenplayService = screenplayService;
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

    /**
     * 查询当前项目最新剧本版本对应的正式剧本。
     *
     * @param projectId 项目 ID
     * @return 最新正式剧本响应
     */
    @GetMapping("/{projectId}/screenplays/latest")
    public ApiResponse<ScreenplayResponse> getLatestScreenplay(@PathVariable @Min(1) Long projectId) {
        log.info("收到查询最新正式剧本请求，项目ID：{}", projectId);
        return ApiResponse.success(screenplayService.getLatestScreenplay(projectId));
    }

    /**
     * 查询指定剧本版本对应的正式剧本。
     *
     * @param projectId 项目 ID
     * @param scriptVersionId 剧本版本 ID
     * @return 指定版本正式剧本响应
     */
    @GetMapping("/{projectId}/screenplays/{scriptVersionId}")
    public ApiResponse<ScreenplayResponse> getScreenplay(
        @PathVariable @Min(1) Long projectId,
        @PathVariable @Min(1) Long scriptVersionId
    ) {
        log.info("收到查询正式剧本请求，项目ID：{}，剧本版本ID：{}", projectId, scriptVersionId);
        return ApiResponse.success(screenplayService.getScreenplay(projectId, scriptVersionId));
    }

    /**
     * 根据指定 YAML 版本重新渲染正式剧本，并保存为快照。
     *
     * @param projectId 项目 ID
     * @param request 正式剧本渲染请求
     * @return 渲染后的正式剧本响应
     */
    @PostMapping("/{projectId}/screenplays/render")
    public ApiResponse<ScreenplayResponse> renderScreenplay(
        @PathVariable @Min(1) Long projectId,
        @Valid @RequestBody ScreenplayRenderRequest request
    ) {
        log.info("收到正式剧本渲染请求，项目ID：{}，剧本版本ID：{}", projectId, request.getScriptVersionId());
        return ApiResponse.success(screenplayService.renderScreenplay(projectId, request.getScriptVersionId()));
    }

    /**
     * 将正式剧本编辑结果同步回 YAML，并生成新的剧本版本。
     *
     * @param projectId 项目 ID
     * @param request 正式剧本同步回写请求
     * @return 新生成的 YAML 版本响应
     */
    @PostMapping("/{projectId}/screenplays/sync-yaml")
    public ApiResponse<AdaptationScriptResponse> syncScreenplayToYaml(
        @PathVariable @Min(1) Long projectId,
        @Valid @RequestBody ScreenplaySyncYamlRequest request
    ) {
        log.info("收到正式剧本同步回 YAML 请求，项目ID：{}，原版本ID：{}", projectId, request.getScriptVersionId());
        return ApiResponse.success(screenplayService.syncScreenplayToYaml(
            projectId,
            request.getScriptVersionId(),
            request.getTitle(),
            request.getMarkdownContent()
        ));
    }

    /**
     * 保存正式剧本编辑结果，并生成新的剧本版本。
     *
     * @param projectId 项目 ID
     * @param request 正式剧本保存请求
     * @return 新生成的 YAML 版本响应
     */
    @PostMapping("/{projectId}/screenplays/save")
    public ApiResponse<AdaptationScriptResponse> saveScreenplay(
        @PathVariable @Min(1) Long projectId,
        @Valid @RequestBody ScreenplaySaveRequest request
    ) {
        log.info("收到正式剧本保存请求，项目ID：{}，原版本ID：{}", projectId, request.getScriptVersionId());
        return ApiResponse.success(screenplayService.saveScreenplay(
            projectId,
            request.getScriptVersionId(),
            request.getTitle(),
            request.getMarkdownContent()
        ));
    }

    /**
     * 导出指定剧本版本对应的正式剧本文本。
     *
     * @param projectId 项目 ID
     * @param scriptVersionId 剧本版本 ID
     * @param format 导出格式
     * @return 文件下载响应
     */
    @GetMapping("/{projectId}/screenplays/{scriptVersionId}/export")
    public ResponseEntity<byte[]> exportScreenplay(
        @PathVariable @Min(1) Long projectId,
        @PathVariable @Min(1) Long scriptVersionId,
        @RequestParam String format
    ) {
        log.info("收到正式剧本导出请求，项目ID：{}，剧本版本ID：{}，格式：{}", projectId, scriptVersionId, format);
        String normalizedFormat = normalizeExportFormat(format);
        String content = "txt".equals(normalizedFormat)
            ? screenplayService.exportPlainText(projectId, scriptVersionId)
            : screenplayService.exportMarkdown(projectId, scriptVersionId);
        ScreenplayResponse screenplayResponse = screenplayService.getScreenplay(projectId, scriptVersionId);
        String fileName = buildExportFileName(screenplayResponse.getTitle(), normalizedFormat);
        String asciiFileName = buildAsciiExportFileName(screenplayResponse.getVersionNo(), normalizedFormat);
        MediaType mediaType = "txt".equals(normalizedFormat)
            ? new MediaType("text", "plain", StandardCharsets.UTF_8)
            : new MediaType("text", "markdown", StandardCharsets.UTF_8);

        return ResponseEntity.ok()
            .contentType(mediaType)
            .header(HttpHeaders.CONTENT_DISPOSITION, buildContentDispositionHeader(asciiFileName, fileName))
            .body(content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 规范化导出格式参数。
     *
     * @param format 原始导出格式
     * @return 规范化后的导出格式
     */
    private String normalizeExportFormat(String format) {
        if (!"md".equalsIgnoreCase(format) && !"txt".equalsIgnoreCase(format)) {
            throw new IllegalArgumentException("导出格式仅支持 md 或 txt。");
        }
        return format.toLowerCase();
    }

    /**
     * 构建导出文件名。
     *
     * @param title 剧本标题
     * @param format 导出格式
     * @return 导出文件名
     */
    private String buildExportFileName(String title, String format) {
        String safeTitle = title == null ? "screenplay" : title.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        if (safeTitle.isBlank()) {
            safeTitle = "screenplay";
        }
        return safeTitle + "." + format;
    }

    /**
     * 构建仅包含 ASCII 字符的回退文件名，避免 Servlet 容器在响应头编码阶段报错。
     *
     * @param versionNo 剧本版本号
     * @param format 导出格式
     * @return 仅包含 ASCII 字符的安全文件名
     */
    private String buildAsciiExportFileName(Integer versionNo, String format) {
        int safeVersionNo = versionNo == null ? 1 : versionNo;
        return "screenplay-v" + safeVersionNo + "." + format;
    }

    /**
     * 构建同时包含 ASCII 回退名和 UTF-8 扩展名的下载响应头。
     *
     * @param asciiFileName ASCII 回退文件名
     * @param utf8FileName 原始 UTF-8 文件名
     * @return 可直接写入响应头的 Content-Disposition 值
     */
    private String buildContentDispositionHeader(String asciiFileName, String utf8FileName) {
        String encodedFileName = URLEncoder.encode(utf8FileName, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + asciiFileName + "\"; filename*=UTF-8''" + encodedFileName;
    }
}
