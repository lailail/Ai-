package com.qiniuyun.novelscript.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiniuyun.novelscript.common.exception.ResourceNotFoundException;
import com.qiniuyun.novelscript.config.adaptation.AdaptationExecutionProperties;
import com.qiniuyun.novelscript.controller.response.AdaptationJobResponse;
import com.qiniuyun.novelscript.controller.response.AdaptationScriptResponse;
import com.qiniuyun.novelscript.controller.response.ScriptValidationErrorResponse;
import com.qiniuyun.novelscript.domain.entity.AdaptationJob;
import com.qiniuyun.novelscript.domain.entity.ChapterContext;
import com.qiniuyun.novelscript.domain.entity.Project;
import com.qiniuyun.novelscript.domain.entity.ScriptVersion;
import com.qiniuyun.novelscript.domain.entity.SourceChapter;
import com.qiniuyun.novelscript.domain.entity.YamlSnapshot;
import com.qiniuyun.novelscript.mapper.AdaptationJobMapper;
import com.qiniuyun.novelscript.mapper.ChapterContextMapper;
import com.qiniuyun.novelscript.mapper.ProjectMapper;
import com.qiniuyun.novelscript.mapper.ScriptVersionMapper;
import com.qiniuyun.novelscript.mapper.SourceChapterMapper;
import com.qiniuyun.novelscript.mapper.YamlSnapshotMapper;
import com.qiniuyun.novelscript.pipeline.model.ChapterContextResult;
import com.qiniuyun.novelscript.pipeline.model.ChapterNormalizeInput;
import com.qiniuyun.novelscript.pipeline.model.ChapterNormalizeResult;
import com.qiniuyun.novelscript.pipeline.model.GlobalContextMergeResult;
import com.qiniuyun.novelscript.pipeline.model.SchemaValidationError;
import com.qiniuyun.novelscript.pipeline.model.SchemaValidationResult;
import com.qiniuyun.novelscript.pipeline.model.ScriptDocument;
import com.qiniuyun.novelscript.pipeline.model.ScriptDocumentMetadata;
import com.qiniuyun.novelscript.pipeline.model.ScriptDocumentProject;
import com.qiniuyun.novelscript.pipeline.model.ScriptEpisodeResult;
import com.qiniuyun.novelscript.pipeline.model.ScriptOutlineEpisode;
import com.qiniuyun.novelscript.pipeline.model.ScriptOutlineResult;
import com.qiniuyun.novelscript.pipeline.model.ScriptSceneResult;
import com.qiniuyun.novelscript.pipeline.model.StoryBibleResult;
import com.qiniuyun.novelscript.pipeline.step.ChapterContextExtractStep;
import com.qiniuyun.novelscript.pipeline.step.ChapterNormalizeStep;
import com.qiniuyun.novelscript.pipeline.step.GlobalContextMergeStep;
import com.qiniuyun.novelscript.pipeline.step.SceneGenerateStep;
import com.qiniuyun.novelscript.pipeline.step.SchemaValidateStep;
import com.qiniuyun.novelscript.pipeline.step.ScriptOutlinePlanStep;
import com.qiniuyun.novelscript.pipeline.step.StoryBibleBuildStep;
import com.qiniuyun.novelscript.pipeline.step.YamlSerializeStep;
import com.qiniuyun.novelscript.service.AdaptationPipelineService;
import com.qiniuyun.novelscript.service.ContextSnapshotService;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 负责串联小说转剧本主链路的服务实现。
 */
@Slf4j
@Service
public class AdaptationPipelineServiceImpl implements AdaptationPipelineService {

    private static final String JOB_STATUS_RUNNING = "RUNNING";
    private static final String JOB_STATUS_SUCCEEDED = "SUCCEEDED";
    private static final String JOB_STATUS_FAILED = "FAILED";
    private static final String SCHEMA_VERSION = "1.0";
    private static final int MIN_CHAPTER_COUNT = 3;

    private final ProjectMapper projectMapper;
    private final SourceChapterMapper sourceChapterMapper;
    private final ChapterContextMapper chapterContextMapper;
    private final AdaptationJobMapper adaptationJobMapper;
    private final ScriptVersionMapper scriptVersionMapper;
    private final YamlSnapshotMapper yamlSnapshotMapper;
    private final ContextSnapshotService contextSnapshotService;
    private final ChapterNormalizeStep chapterNormalizeStep;
    private final ChapterContextExtractStep chapterContextExtractStep;
    private final GlobalContextMergeStep globalContextMergeStep;
    private final StoryBibleBuildStep storyBibleBuildStep;
    private final ScriptOutlinePlanStep scriptOutlinePlanStep;
    private final SceneGenerateStep sceneGenerateStep;
    private final YamlSerializeStep yamlSerializeStep;
    private final SchemaValidateStep schemaValidateStep;
    private final ObjectMapper objectMapper;
    private final TaskExecutor adaptationTaskExecutor;
    private final AdaptationExecutionProperties adaptationExecutionProperties;

    /**
     * 构造改编流水线编排服务。
     *
     * @param projectMapper 项目 Mapper
     * @param sourceChapterMapper 原始章节 Mapper
     * @param chapterContextMapper 单章上下文 Mapper
     * @param adaptationJobMapper 改编任务 Mapper
     * @param scriptVersionMapper 剧本版本 Mapper
     * @param yamlSnapshotMapper YAML 快照 Mapper
     * @param contextSnapshotService 上下文快照服务
     * @param chapterNormalizeStep 章节标准化步骤
     * @param chapterContextExtractStep 单章上下文抽取步骤
     * @param globalContextMergeStep 全局上下文合并步骤
     * @param storyBibleBuildStep Story Bible 构建步骤
     * @param scriptOutlinePlanStep 剧本大纲规划步骤
     * @param sceneGenerateStep 场景生成步骤
     * @param yamlSerializeStep YAML 序列化步骤
     * @param schemaValidateStep Schema 校验步骤
     * @param objectMapper JSON 读写工具
     * @param adaptationTaskExecutor 改编任务执行器
     * @param adaptationExecutionProperties 改编执行配置
     */
    public AdaptationPipelineServiceImpl(
        ProjectMapper projectMapper,
        SourceChapterMapper sourceChapterMapper,
        ChapterContextMapper chapterContextMapper,
        AdaptationJobMapper adaptationJobMapper,
        ScriptVersionMapper scriptVersionMapper,
        YamlSnapshotMapper yamlSnapshotMapper,
        ContextSnapshotService contextSnapshotService,
        ChapterNormalizeStep chapterNormalizeStep,
        ChapterContextExtractStep chapterContextExtractStep,
        GlobalContextMergeStep globalContextMergeStep,
        StoryBibleBuildStep storyBibleBuildStep,
        ScriptOutlinePlanStep scriptOutlinePlanStep,
        SceneGenerateStep sceneGenerateStep,
        YamlSerializeStep yamlSerializeStep,
        SchemaValidateStep schemaValidateStep,
        ObjectMapper objectMapper,
        TaskExecutor adaptationTaskExecutor,
        AdaptationExecutionProperties adaptationExecutionProperties
    ) {
        this.projectMapper = projectMapper;
        this.sourceChapterMapper = sourceChapterMapper;
        this.chapterContextMapper = chapterContextMapper;
        this.adaptationJobMapper = adaptationJobMapper;
        this.scriptVersionMapper = scriptVersionMapper;
        this.yamlSnapshotMapper = yamlSnapshotMapper;
        this.contextSnapshotService = contextSnapshotService;
        this.chapterNormalizeStep = chapterNormalizeStep;
        this.chapterContextExtractStep = chapterContextExtractStep;
        this.globalContextMergeStep = globalContextMergeStep;
        this.storyBibleBuildStep = storyBibleBuildStep;
        this.scriptOutlinePlanStep = scriptOutlinePlanStep;
        this.sceneGenerateStep = sceneGenerateStep;
        this.yamlSerializeStep = yamlSerializeStep;
        this.schemaValidateStep = schemaValidateStep;
        this.objectMapper = objectMapper;
        this.adaptationTaskExecutor = adaptationTaskExecutor;
        this.adaptationExecutionProperties = adaptationExecutionProperties;
    }

    /**
     * 启动指定项目的改编任务。
     *
     * @param projectId 项目 ID
     * @return 任务启动后的进度响应
     */
    @Override
    public AdaptationJobResponse startAdaptation(Long projectId) {
        loadProject(projectId);
        loadProjectChapters(projectId);

        AdaptationJob runningJob = findRunningJob(projectId);
        if (runningJob != null) {
            log.info("【改编编排】检测到进行中的任务，直接复用，项目ID：{}，任务ID：{}", projectId, runningJob.getId());
            return AdaptationJobResponse.from(runningJob);
        }

        AdaptationJob job = createRunningJob(projectId);
        if (adaptationExecutionProperties.isAsyncEnabled()) {
            adaptationTaskExecutor.execute(() -> executePipelineSafely(projectId, job.getId()));
            return AdaptationJobResponse.from(job);
        }

        executePipelineSafely(projectId, job.getId());
        return AdaptationJobResponse.from(loadJob(job.getId()));
    }

    /**
     * 查询指定项目当前最新的改编任务。
     *
     * @param projectId 项目 ID
     * @return 最新任务进度响应
     */
    @Override
    @Transactional(readOnly = true)
    public AdaptationJobResponse getLatestJob(Long projectId) {
        loadProject(projectId);
        AdaptationJob job = findLatestJob(projectId);
        if (job == null) {
            throw new ResourceNotFoundException("当前项目还没有改编任务记录。");
        }
        return AdaptationJobResponse.from(job);
    }

    /**
     * 查询指定项目当前最新的剧本版本。
     *
     * @param projectId 项目 ID
     * @return 最新剧本响应
     */
    @Override
    @Transactional(readOnly = true)
    public AdaptationScriptResponse getLatestScript(Long projectId) {
        loadProject(projectId);

        ScriptVersion scriptVersion = scriptVersionMapper.selectOne(
            new LambdaQueryWrapper<ScriptVersion>()
                .eq(ScriptVersion::getProjectId, projectId)
                .orderByDesc(ScriptVersion::getVersionNo)
                .last("limit 1")
        );
        if (scriptVersion == null) {
            throw new ResourceNotFoundException("当前项目还没有生成剧本版本。");
        }

        YamlSnapshot yamlSnapshot = yamlSnapshotMapper.selectOne(
            new LambdaQueryWrapper<YamlSnapshot>()
                .eq(YamlSnapshot::getProjectId, projectId)
                .eq(YamlSnapshot::getScriptVersionId, scriptVersion.getId())
                .orderByDesc(YamlSnapshot::getId)
                .last("limit 1")
        );
        if (yamlSnapshot == null) {
            throw new ResourceNotFoundException("当前项目缺少对应的 YAML 快照。");
        }

        log.info("【改编编排】查询最新剧本成功，项目ID：{}，版本号：{}", projectId, scriptVersion.getVersionNo());
        return AdaptationScriptResponse.from(
            projectId,
            scriptVersion,
            yamlSnapshot,
            null,
            parseValidationErrors(yamlSnapshot.getValidationErrors())
        );
    }

    /**
     * 安全执行后台改编流水线，并负责兜底失败状态。
     *
     * @param projectId 项目 ID
     * @param jobId 任务 ID
     */
    private void executePipelineSafely(Long projectId, Long jobId) {
        try {
            runPipeline(projectId, jobId);
        }
        catch (RuntimeException exception) {
            AdaptationJob job = loadJob(jobId);
            if (!JOB_STATUS_FAILED.equals(job.getStatus())) {
                markJobFailed(job, job.getCurrentStage(), exception.getMessage());
            }
            log.error("【改编编排】后台任务执行异常，项目ID：{}，任务ID：{}", projectId, jobId, exception);
        }
    }

    /**
     * 执行完整的小说转剧本流水线。
     *
     * @param projectId 项目 ID
     * @param jobId 任务 ID
     */
    private void runPipeline(Long projectId, Long jobId) {
        Project project = loadProject(projectId);
        List<SourceChapter> sourceChapters = loadProjectChapters(projectId);
        AdaptationJob job = loadJob(jobId);

        ChapterNormalizeResult normalizedResult = executeChapterNormalize(job, projectId, sourceChapters);
        List<ChapterContextResult> chapterContexts = executeChapterContextExtract(job, normalizedResult);
        contextSnapshotService.saveChapterContexts(chapterContexts);

        List<Long> sourceContextIds = loadChapterContextIds(projectId, sourceChapters);
        GlobalContextMergeResult globalContext = executeGlobalContextMerge(job, projectId, chapterContexts);
        StoryBibleResult storyBible = executeStoryBibleBuild(job, projectId, globalContext);
        contextSnapshotService.saveStoryBible(storyBible, sourceContextIds);

        ScriptOutlineResult outlineResult = executeScriptOutlinePlan(job, projectId, storyBible, chapterContexts);
        List<ScriptEpisodeResult> episodes = executeSceneGenerate(job, storyBible, outlineResult);
        ScriptDocument scriptDocument = buildScriptDocument(project, sourceChapters, storyBible, episodes);

        String yamlContent = executeYamlSerialize(job, scriptDocument);
        SchemaValidationResult validationResult = executeSchemaValidate(job, yamlContent);
        if (!validationResult.isValid()) {
            String summary = buildValidationSummary(validationResult);
            markJobFailed(job, "SCHEMA_VALIDATE", summary);
            throw new IllegalStateException("剧本 YAML 校验失败：" + summary);
        }

        updateJobStage(job, "VERSION_SAVE");
        saveScriptVersion(project, yamlContent, validationResult, episodes);
        markJobSucceeded(job);
        log.info("【改编编排】项目改编完成，项目ID：{}，任务ID：{}", projectId, jobId);
    }

    /**
     * 执行章节标准化阶段。
     *
     * @param job 当前改编任务
     * @param projectId 项目 ID
     * @param sourceChapters 原始章节列表
     * @return 章节标准化结果
     */
    private ChapterNormalizeResult executeChapterNormalize(
        AdaptationJob job,
        Long projectId,
        List<SourceChapter> sourceChapters
    ) {
        updateJobStage(job, "CHAPTER_NORMALIZE");
        ChapterNormalizeInput input = new ChapterNormalizeInput();
        input.setProjectId(projectId);
        input.setChapters(sourceChapters);
        return chapterNormalizeStep.execute(input);
    }

    /**
     * 执行单章上下文抽取阶段。
     *
     * @param job 当前改编任务
     * @param normalizedResult 章节标准化结果
     * @return 单章上下文结果列表
     */
    private List<ChapterContextResult> executeChapterContextExtract(
        AdaptationJob job,
        ChapterNormalizeResult normalizedResult
    ) {
        updateJobStage(job, "CHAPTER_CONTEXT_EXTRACT");
        List<ChapterContextResult> chapterContexts = new ArrayList<>();
        normalizedResult.getNormalizedChapters().forEach(chapter ->
            chapterContexts.add(chapterContextExtractStep.execute(chapter))
        );
        return chapterContexts;
    }

    /**
     * 执行全局上下文合并阶段。
     *
     * @param job 当前改编任务
     * @param projectId 项目 ID
     * @param chapterContexts 单章上下文结果列表
     * @return 全局上下文结果
     */
    private GlobalContextMergeResult executeGlobalContextMerge(
        AdaptationJob job,
        Long projectId,
        List<ChapterContextResult> chapterContexts
    ) {
        updateJobStage(job, "GLOBAL_CONTEXT_MERGE");
        return globalContextMergeStep.execute(projectId, chapterContexts);
    }

    /**
     * 执行 Story Bible 构建阶段。
     *
     * @param job 当前改编任务
     * @param projectId 项目 ID
     * @param globalContext 全局上下文结果
     * @return Story Bible 结果
     */
    private StoryBibleResult executeStoryBibleBuild(
        AdaptationJob job,
        Long projectId,
        GlobalContextMergeResult globalContext
    ) {
        updateJobStage(job, "STORY_BIBLE_BUILD");
        return storyBibleBuildStep.execute(projectId, globalContext);
    }

    /**
     * 执行剧本大纲规划阶段。
     *
     * @param job 当前改编任务
     * @param projectId 项目 ID
     * @param storyBible Story Bible 结果
     * @param chapterContexts 单章上下文结果列表
     * @return 大纲规划结果
     */
    private ScriptOutlineResult executeScriptOutlinePlan(
        AdaptationJob job,
        Long projectId,
        StoryBibleResult storyBible,
        List<ChapterContextResult> chapterContexts
    ) {
        updateJobStage(job, "SCRIPT_OUTLINE_PLAN");
        return scriptOutlinePlanStep.execute(projectId, storyBible, chapterContexts);
    }

    /**
     * 执行逐场场景生成阶段。
     *
     * @param job 当前改编任务
     * @param storyBible Story Bible 结果
     * @param outlineResult 大纲规划结果
     * @return 剧集结果列表
     */
    private List<ScriptEpisodeResult> executeSceneGenerate(
        AdaptationJob job,
        StoryBibleResult storyBible,
        ScriptOutlineResult outlineResult
    ) {
        updateJobStage(job, "SCENE_GENERATE");
        List<ScriptEpisodeResult> episodeResults = new ArrayList<>();
        for (ScriptOutlineEpisode episode : outlineResult.getEpisodes()) {
            List<ScriptSceneResult> scenes = new ArrayList<>();
            episode.getScenes().forEach(scenePlan -> scenes.add(sceneGenerateStep.execute(storyBible, scenePlan)));
            episodeResults.add(
                ScriptEpisodeResult.fromOutline(
                    episode.getId(),
                    episode.getTitle(),
                    episode.getPremise(),
                    episode.getSourceRefs(),
                    scenes
                )
            );
        }
        return episodeResults;
    }

    /**
     * 执行 YAML 序列化阶段。
     *
     * @param job 当前改编任务
     * @param scriptDocument 结构化剧本文档
     * @return YAML 文本
     */
    private String executeYamlSerialize(AdaptationJob job, ScriptDocument scriptDocument) {
        updateJobStage(job, "YAML_SERIALIZE");
        return yamlSerializeStep.execute(scriptDocument);
    }

    /**
     * 执行 Schema 校验阶段。
     *
     * @param job 当前改编任务
     * @param yamlContent YAML 文本
     * @return Schema 校验结果
     */
    private SchemaValidationResult executeSchemaValidate(AdaptationJob job, String yamlContent) {
        updateJobStage(job, "SCHEMA_VALIDATE");
        return schemaValidateStep.execute(yamlContent);
    }

    /**
     * 加载指定项目实体。
     *
     * @param projectId 项目 ID
     * @return 项目实体
     */
    private Project loadProject(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new ResourceNotFoundException("项目不存在。");
        }
        return project;
    }

    /**
     * 加载项目下全部原始章节，并按章节号升序返回。
     *
     * @param projectId 项目 ID
     * @return 原始章节列表
     */
    private List<SourceChapter> loadProjectChapters(Long projectId) {
        List<SourceChapter> sourceChapters = sourceChapterMapper.selectList(
            new LambdaQueryWrapper<SourceChapter>()
                .eq(SourceChapter::getProjectId, projectId)
                .orderByAsc(SourceChapter::getChapterNo)
        );
        if (CollectionUtils.isEmpty(sourceChapters)) {
            throw new IllegalArgumentException("当前项目还没有可用于改编的章节。");
        }
        if (sourceChapters.size() < MIN_CHAPTER_COUNT) {
            throw new IllegalArgumentException("至少需要录入 3 章内容后才能开始改编。");
        }
        return sourceChapters;
    }

    /**
     * 按任务 ID 加载改编任务实体。
     *
     * @param jobId 任务 ID
     * @return 改编任务实体
     */
    private AdaptationJob loadJob(Long jobId) {
        AdaptationJob job = adaptationJobMapper.selectById(jobId);
        if (job == null) {
            throw new ResourceNotFoundException("改编任务不存在。");
        }
        return job;
    }

    /**
     * 查询项目下仍在执行中的任务。
     *
     * @param projectId 项目 ID
     * @return 运行中的任务；若不存在则返回 null
     */
    private AdaptationJob findRunningJob(Long projectId) {
        return adaptationJobMapper.selectOne(
            new LambdaQueryWrapper<AdaptationJob>()
                .eq(AdaptationJob::getProjectId, projectId)
                .eq(AdaptationJob::getStatus, JOB_STATUS_RUNNING)
                .orderByDesc(AdaptationJob::getId)
                .last("limit 1")
        );
    }

    /**
     * 查询项目下最新一条改编任务。
     *
     * @param projectId 项目 ID
     * @return 最新任务；若不存在则返回 null
     */
    private AdaptationJob findLatestJob(Long projectId) {
        return adaptationJobMapper.selectOne(
            new LambdaQueryWrapper<AdaptationJob>()
                .eq(AdaptationJob::getProjectId, projectId)
                .orderByDesc(AdaptationJob::getId)
                .last("limit 1")
        );
    }

    /**
     * 创建运行中的改编任务记录。
     *
     * @param projectId 项目 ID
     * @return 新建的改编任务实体
     */
    private AdaptationJob createRunningJob(Long projectId) {
        AdaptationJob job = new AdaptationJob();
        job.setProjectId(projectId);
        job.setStatus(JOB_STATUS_RUNNING);
        job.setCurrentStage("CREATED");
        job.setStartedAt(LocalDateTime.now());
        adaptationJobMapper.insert(job);
        log.info("【改编编排】创建改编任务成功，项目ID：{}，任务ID：{}", projectId, job.getId());
        return job;
    }

    /**
     * 更新当前任务所在阶段。
     *
     * @param job 当前改编任务
     * @param stage 当前阶段标识
     */
    private void updateJobStage(AdaptationJob job, String stage) {
        job.setCurrentStage(stage);
        adaptationJobMapper.updateById(job);
        log.info("【改编编排】任务进入阶段，任务ID：{}，阶段：{}", job.getId(), stage);
    }

    /**
     * 将任务标记为成功完成。
     *
     * @param job 当前改编任务
     */
    private void markJobSucceeded(AdaptationJob job) {
        job.setStatus(JOB_STATUS_SUCCEEDED);
        job.setCurrentStage("COMPLETED");
        job.setFinishedAt(LocalDateTime.now());
        adaptationJobMapper.updateById(job);
        log.info("【改编编排】任务执行成功，任务ID：{}", job.getId());
    }

    /**
     * 将任务标记为失败，并记录失败阶段与摘要。
     *
     * @param job 当前改编任务
     * @param stage 失败阶段
     * @param errorMessage 错误摘要
     */
    private void markJobFailed(AdaptationJob job, String stage, String errorMessage) {
        job.setStatus(JOB_STATUS_FAILED);
        job.setErrorStage(stage);
        job.setErrorMessage(StringUtils.hasText(errorMessage) ? errorMessage : "未知错误");
        job.setFinishedAt(LocalDateTime.now());
        adaptationJobMapper.updateById(job);
        log.warn("【改编编排】任务执行失败，任务ID：{}，阶段：{}，原因：{}", job.getId(), stage, job.getErrorMessage());
    }

    /**
     * 读取项目下已保存的单章上下文快照 ID 列表。
     *
     * @param projectId 项目 ID
     * @param sourceChapters 原始章节列表
     * @return 上下文快照 ID 列表
     */
    private List<Long> loadChapterContextIds(Long projectId, List<SourceChapter> sourceChapters) {
        List<Long> chapterIds = sourceChapters.stream()
            .map(SourceChapter::getId)
            .filter(Objects::nonNull)
            .toList();
        if (CollectionUtils.isEmpty(chapterIds)) {
            return List.of();
        }

        return chapterContextMapper.selectList(
            new LambdaQueryWrapper<ChapterContext>()
                .eq(ChapterContext::getProjectId, projectId)
                .in(ChapterContext::getChapterId, chapterIds)
                .orderByAsc(ChapterContext::getId)
        ).stream().map(ChapterContext::getId).toList();
    }

    /**
     * 组装最终的结构化剧本文档对象。
     *
     * @param project 项目实体
     * @param sourceChapters 原始章节列表
     * @param storyBible Story Bible 结果
     * @param episodes 剧集结果列表
     * @return 结构化剧本文档
     */
    private ScriptDocument buildScriptDocument(
        Project project,
        List<SourceChapter> sourceChapters,
        StoryBibleResult storyBible,
        List<ScriptEpisodeResult> episodes
    ) {
        ScriptDocument document = new ScriptDocument();
        document.setSchemaVersion(SCHEMA_VERSION);
        document.setProject(buildScriptDocumentProject(project, sourceChapters));
        document.setStoryBible(storyBible);
        document.setEpisodes(episodes);
        document.setMetadata(buildMetadata());
        return document;
    }

    /**
     * 构造剧本文档中的项目基础信息节点。
     *
     * @param project 项目实体
     * @param sourceChapters 原始章节列表
     * @return 项目基础信息节点
     */
    private ScriptDocumentProject buildScriptDocumentProject(Project project, List<SourceChapter> sourceChapters) {
        ScriptDocumentProject documentProject = new ScriptDocumentProject();
        documentProject.setId(String.valueOf(project.getId()));
        documentProject.setTitle(project.getTitle());
        documentProject.setSourceChapters(
            sourceChapters.stream()
                .map(SourceChapter::getChapterNo)
                .filter(Objects::nonNull)
                .toList()
        );
        documentProject.setAdaptationMode("screenplay");
        return documentProject;
    }

    /**
     * 构造剧本文档元数据节点。
     *
     * @return 文档元数据
     */
    private ScriptDocumentMetadata buildMetadata() {
        ScriptDocumentMetadata metadata = new ScriptDocumentMetadata();
        metadata.setGeneratedAt(OffsetDateTime.now());
        metadata.setGenerator("spring-ai-deepseek");
        metadata.setNotes(List.of("AI 自动生成的剧本初稿"));
        return metadata;
    }

    /**
     * 保存新的剧本版本和 YAML 快照。
     *
     * @param project 项目实体
     * @param yamlContent YAML 文本
     * @param validationResult Schema 校验结果
     * @param episodes 剧集结果列表
     * @return 保存后的剧本响应
     */
    private AdaptationScriptResponse saveScriptVersion(
        Project project,
        String yamlContent,
        SchemaValidationResult validationResult,
        List<ScriptEpisodeResult> episodes
    ) {
        LocalDateTime now = LocalDateTime.now();

        ScriptVersion scriptVersion = new ScriptVersion();
        scriptVersion.setProjectId(project.getId());
        scriptVersion.setVersionNo(resolveNextVersionNo(project.getId()));
        scriptVersion.setSourceType("AI_GENERATED");
        scriptVersion.setTitle(resolveVersionTitle(project, episodes));
        scriptVersion.setCreatedAt(now);
        scriptVersion.setUpdatedAt(now);
        scriptVersionMapper.insert(scriptVersion);

        YamlSnapshot yamlSnapshot = new YamlSnapshot();
        yamlSnapshot.setProjectId(project.getId());
        yamlSnapshot.setScriptVersionId(scriptVersion.getId());
        yamlSnapshot.setSchemaVersion(SCHEMA_VERSION);
        yamlSnapshot.setYamlContent(yamlContent);
        yamlSnapshot.setValidationStatus(validationResult.isValid() ? "PASSED" : "FAILED");
        yamlSnapshot.setValidationErrors(writeAsJson(validationResult.getErrors()));
        yamlSnapshot.setCreatedAt(now);
        yamlSnapshot.setUpdatedAt(now);
        yamlSnapshotMapper.insert(yamlSnapshot);

        log.info("【改编编排】剧本版本保存完成，项目ID：{}，版本号：{}", project.getId(), scriptVersion.getVersionNo());
        return AdaptationScriptResponse.from(
            project.getId(),
            scriptVersion,
            yamlSnapshot,
            null,
            validationResult.getErrors().stream().map(ScriptValidationErrorResponse::from).toList()
        );
    }

    /**
     * 计算项目下一个剧本版本号。
     *
     * @param projectId 项目 ID
     * @return 下一个版本号
     */
    private Integer resolveNextVersionNo(Long projectId) {
        ScriptVersion latestScriptVersion = scriptVersionMapper.selectOne(
            new LambdaQueryWrapper<ScriptVersion>()
                .eq(ScriptVersion::getProjectId, projectId)
                .orderByDesc(ScriptVersion::getVersionNo)
                .last("limit 1")
        );
        if (latestScriptVersion == null || latestScriptVersion.getVersionNo() == null) {
            return 1;
        }
        return latestScriptVersion.getVersionNo() + 1;
    }

    /**
     * 生成当前剧本版本标题。
     *
     * @param project 项目实体
     * @param episodes 剧集结果列表
     * @return 版本标题
     */
    private String resolveVersionTitle(Project project, List<ScriptEpisodeResult> episodes) {
        if (!CollectionUtils.isEmpty(episodes) && StringUtils.hasText(episodes.get(0).getTitle())) {
            return project.getTitle() + " - " + episodes.get(0).getTitle();
        }
        return project.getTitle() + " - 剧本初稿";
    }

    /**
     * 生成 Schema 校验错误摘要。
     *
     * @param validationResult Schema 校验结果
     * @return 校验摘要
     */
    private String buildValidationSummary(SchemaValidationResult validationResult) {
        return validationResult.getErrors().stream()
            .limit(3)
            .map(this::formatValidationError)
            .reduce((left, right) -> left + "；" + right)
            .orElse("未通过 Schema 校验");
    }

    /**
     * 将单条校验错误格式化为摘要文本。
     *
     * @param error 单条校验错误
     * @return 格式化后的错误文本
     */
    private String formatValidationError(SchemaValidationError error) {
        return error.getPath() + ": " + error.getMessage();
    }

    /**
     * 解析落库保存的 YAML 校验错误 JSON。
     *
     * @param validationErrorsJson 落库的校验错误 JSON
     * @return 面向前端的结构化错误列表
     */
    private List<ScriptValidationErrorResponse> parseValidationErrors(String validationErrorsJson) {
        if (!StringUtils.hasText(validationErrorsJson)) {
            return List.of();
        }

        try {
            List<SchemaValidationError> errors = objectMapper.readValue(
                validationErrorsJson,
                new TypeReference<List<SchemaValidationError>>() { }
            );
            return errors.stream().map(ScriptValidationErrorResponse::from).toList();
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("剧本校验错误反序列化失败。", exception);
        }
    }

    /**
     * 将对象序列化为 JSON 字符串。
     *
     * @param value 待序列化对象
     * @return JSON 字符串
     */
    private String writeAsJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("结果序列化失败。", exception);
        }
    }
}
