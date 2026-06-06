package com.qiniuyun.novelscript.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiniuyun.novelscript.common.exception.ResourceNotFoundException;
import com.qiniuyun.novelscript.controller.response.AdaptationScriptResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 负责串联小说转剧本最小主链路的服务实现。
 */
@Slf4j
@Service
public class AdaptationPipelineServiceImpl implements AdaptationPipelineService {

    private static final String JOB_STATUS_RUNNING = "RUNNING";
    private static final String JOB_STATUS_SUCCEEDED = "SUCCEEDED";
    private static final String JOB_STATUS_FAILED = "FAILED";
    private static final String SCHEMA_VERSION = "1.0";

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
        ObjectMapper objectMapper
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
    }

    /**
     * 同步执行最小改编链路，并返回新生成的剧本版本。
     *
     * @param projectId 项目 ID
     * @return 新生成的剧本响应
     */
    @Override
    public AdaptationScriptResponse generateScript(Long projectId) {
        Project project = loadProject(projectId);
        List<SourceChapter> sourceChapters = loadProjectChapters(projectId);
        AdaptationJob job = createRunningJob(projectId);

        try {
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
            AdaptationScriptResponse response = saveScriptVersion(project, yamlContent, validationResult, episodes);
            markJobSucceeded(job);
            response.setJobId(job.getId());
            response.setJobStatus(job.getStatus());
            log.info("【改编编排】项目改编完成，项目ID：{}，版本号：{}", projectId, response.getVersionNo());
            return response;
        }
        catch (RuntimeException exception) {
            if (!JOB_STATUS_FAILED.equals(job.getStatus())) {
                markJobFailed(job, job.getCurrentStage(), exception.getMessage());
            }
            throw exception;
        }
    }

    /**
     * 查询指定项目最新的剧本版本。
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
            throw new ResourceNotFoundException("当前项目还没有生成剧本版本");
        }

        YamlSnapshot yamlSnapshot = yamlSnapshotMapper.selectOne(
            new LambdaQueryWrapper<YamlSnapshot>()
                .eq(YamlSnapshot::getProjectId, projectId)
                .eq(YamlSnapshot::getScriptVersionId, scriptVersion.getId())
                .orderByDesc(YamlSnapshot::getId)
                .last("limit 1")
        );
        if (yamlSnapshot == null) {
            throw new ResourceNotFoundException("当前项目缺少对应的 YAML 快照");
        }

        log.info("【改编编排】查询最新剧本成功，项目ID：{}，版本号：{}", projectId, scriptVersion.getVersionNo());
        return AdaptationScriptResponse.from(projectId, scriptVersion, yamlSnapshot, null);
    }

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

    private GlobalContextMergeResult executeGlobalContextMerge(
        AdaptationJob job,
        Long projectId,
        List<ChapterContextResult> chapterContexts
    ) {
        updateJobStage(job, "GLOBAL_CONTEXT_MERGE");
        return globalContextMergeStep.execute(projectId, chapterContexts);
    }

    private StoryBibleResult executeStoryBibleBuild(
        AdaptationJob job,
        Long projectId,
        GlobalContextMergeResult globalContext
    ) {
        updateJobStage(job, "STORY_BIBLE_BUILD");
        return storyBibleBuildStep.execute(projectId, globalContext);
    }

    private ScriptOutlineResult executeScriptOutlinePlan(
        AdaptationJob job,
        Long projectId,
        StoryBibleResult storyBible,
        List<ChapterContextResult> chapterContexts
    ) {
        updateJobStage(job, "SCRIPT_OUTLINE_PLAN");
        return scriptOutlinePlanStep.execute(projectId, storyBible, chapterContexts);
    }

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

    private String executeYamlSerialize(AdaptationJob job, ScriptDocument scriptDocument) {
        updateJobStage(job, "YAML_SERIALIZE");
        return yamlSerializeStep.execute(scriptDocument);
    }

    private SchemaValidationResult executeSchemaValidate(AdaptationJob job, String yamlContent) {
        updateJobStage(job, "SCHEMA_VALIDATE");
        return schemaValidateStep.execute(yamlContent);
    }

    private Project loadProject(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new ResourceNotFoundException("项目不存在");
        }
        return project;
    }

    private List<SourceChapter> loadProjectChapters(Long projectId) {
        List<SourceChapter> sourceChapters = sourceChapterMapper.selectList(
            new LambdaQueryWrapper<SourceChapter>()
                .eq(SourceChapter::getProjectId, projectId)
                .orderByAsc(SourceChapter::getChapterNo)
        );
        if (CollectionUtils.isEmpty(sourceChapters)) {
            throw new IllegalArgumentException("当前项目还没有可用于改编的章节");
        }
        return sourceChapters;
    }

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

    private void updateJobStage(AdaptationJob job, String stage) {
        job.setCurrentStage(stage);
        adaptationJobMapper.updateById(job);
        log.info("【改编编排】任务进入阶段，任务ID：{}，阶段：{}", job.getId(), stage);
    }

    private void markJobSucceeded(AdaptationJob job) {
        job.setStatus(JOB_STATUS_SUCCEEDED);
        job.setCurrentStage("COMPLETED");
        job.setFinishedAt(LocalDateTime.now());
        adaptationJobMapper.updateById(job);
        log.info("【改编编排】任务执行成功，任务ID：{}", job.getId());
    }

    private void markJobFailed(AdaptationJob job, String stage, String errorMessage) {
        job.setStatus(JOB_STATUS_FAILED);
        job.setErrorStage(stage);
        job.setErrorMessage(StringUtils.hasText(errorMessage) ? errorMessage : "未知错误");
        job.setFinishedAt(LocalDateTime.now());
        adaptationJobMapper.updateById(job);
        log.warn("【改编编排】任务执行失败，任务ID：{}，阶段：{}，原因：{}", job.getId(), stage, job.getErrorMessage());
    }

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

    private ScriptDocumentMetadata buildMetadata() {
        ScriptDocumentMetadata metadata = new ScriptDocumentMetadata();
        metadata.setGeneratedAt(OffsetDateTime.now());
        metadata.setGenerator("spring-ai-deepseek");
        metadata.setNotes(List.of("AI 自动生成的剧本初稿"));
        return metadata;
    }

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
        return AdaptationScriptResponse.from(project.getId(), scriptVersion, yamlSnapshot, null);
    }

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

    private String resolveVersionTitle(Project project, List<ScriptEpisodeResult> episodes) {
        if (!CollectionUtils.isEmpty(episodes) && StringUtils.hasText(episodes.get(0).getTitle())) {
            return project.getTitle() + " - " + episodes.get(0).getTitle();
        }
        return project.getTitle() + " - 剧本初稿";
    }

    private String buildValidationSummary(SchemaValidationResult validationResult) {
        return validationResult.getErrors().stream()
            .limit(3)
            .map(this::formatValidationError)
            .reduce((left, right) -> left + "；" + right)
            .orElse("未通过 Schema 校验");
    }

    private String formatValidationError(SchemaValidationError error) {
        return error.getPath() + ": " + error.getMessage();
    }

    private String writeAsJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("结果序列化失败", exception);
        }
    }
}
