package com.qiniuyun.novelscript.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiniuyun.novelscript.common.exception.ResourceNotFoundException;
import com.qiniuyun.novelscript.controller.response.AdaptationScriptResponse;
import com.qiniuyun.novelscript.controller.response.ScriptValidationErrorResponse;
import com.qiniuyun.novelscript.controller.response.ScriptValidationResponse;
import com.qiniuyun.novelscript.controller.response.ScriptVersionSummaryResponse;
import com.qiniuyun.novelscript.domain.entity.Project;
import com.qiniuyun.novelscript.domain.entity.ScriptVersion;
import com.qiniuyun.novelscript.domain.entity.YamlSnapshot;
import com.qiniuyun.novelscript.mapper.ProjectMapper;
import com.qiniuyun.novelscript.mapper.ScriptVersionMapper;
import com.qiniuyun.novelscript.mapper.YamlSnapshotMapper;
import com.qiniuyun.novelscript.pipeline.model.SchemaValidationError;
import com.qiniuyun.novelscript.pipeline.model.SchemaValidationResult;
import com.qiniuyun.novelscript.pipeline.step.SchemaValidateStep;
import com.qiniuyun.novelscript.service.ScriptVersionService;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 负责 YAML 剧本版本的查询、校验和保存实现。
 */
@Slf4j
@Service
public class ScriptVersionServiceImpl implements ScriptVersionService {

    private static final String SCHEMA_VERSION = "1.0";
    private static final String SOURCE_TYPE_USER_EDITED = "USER_EDITED";

    private final ProjectMapper projectMapper;
    private final ScriptVersionMapper scriptVersionMapper;
    private final YamlSnapshotMapper yamlSnapshotMapper;
    private final SchemaValidateStep schemaValidateStep;
    private final ObjectMapper objectMapper;

    /**
     * 构造剧本版本服务实现。
     *
     * @param projectMapper 项目 Mapper
     * @param scriptVersionMapper 剧本版本 Mapper
     * @param yamlSnapshotMapper YAML 快照 Mapper
     * @param schemaValidateStep YAML Schema 校验步骤
     * @param objectMapper JSON 工具
     */
    public ScriptVersionServiceImpl(
        ProjectMapper projectMapper,
        ScriptVersionMapper scriptVersionMapper,
        YamlSnapshotMapper yamlSnapshotMapper,
        SchemaValidateStep schemaValidateStep,
        ObjectMapper objectMapper
    ) {
        this.projectMapper = projectMapper;
        this.scriptVersionMapper = scriptVersionMapper;
        this.yamlSnapshotMapper = yamlSnapshotMapper;
        this.schemaValidateStep = schemaValidateStep;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询当前项目下的全部剧本版本摘要。
     *
     * @param projectId 项目 ID
     * @return 按版本号倒序排列的剧本版本摘要
     */
    @Override
    @Transactional(readOnly = true)
    public List<ScriptVersionSummaryResponse> listScriptVersions(Long projectId) {
        loadProject(projectId);
        List<ScriptVersion> scriptVersions = loadScriptVersions(projectId);
        if (CollectionUtils.isEmpty(scriptVersions)) {
            return List.of();
        }

        Map<Long, YamlSnapshot> latestSnapshotMap = buildLatestSnapshotMap(projectId, scriptVersions);
        Long latestScriptVersionId = scriptVersions.get(0).getId();
        return scriptVersions.stream()
            .map(scriptVersion -> ScriptVersionSummaryResponse.from(
                projectId,
                scriptVersion,
                latestSnapshotMap.get(scriptVersion.getId()),
                scriptVersion.getId().equals(latestScriptVersionId)
            ))
            .toList();
    }

    /**
     * 查询指定剧本版本的 YAML 详情。
     *
     * @param projectId 项目 ID
     * @param scriptVersionId 剧本版本 ID
     * @return 指定版本的 YAML 详情
     */
    @Override
    @Transactional(readOnly = true)
    public AdaptationScriptResponse getScriptVersion(Long projectId, Long scriptVersionId) {
        loadProject(projectId);
        ScriptVersion scriptVersion = loadScriptVersion(projectId, scriptVersionId);
        YamlSnapshot yamlSnapshot = loadLatestYamlSnapshot(projectId, scriptVersionId);
        List<ScriptValidationErrorResponse> validationErrors = parseValidationErrors(yamlSnapshot.getValidationErrors());
        log.info(
            "【剧本版本】查询指定版本成功，项目ID：{}，版本ID：{}，版本号：{}",
            projectId,
            scriptVersionId,
            scriptVersion.getVersionNo()
        );
        return AdaptationScriptResponse.from(projectId, scriptVersion, yamlSnapshot, null, validationErrors);
    }

    /**
     * 对任意 YAML 原文执行后端 Schema 校验。
     *
     * @param projectId 项目 ID
     * @param yamlContent 待校验的 YAML 原文
     * @return 结构化校验结果
     */
    @Override
    @Transactional(readOnly = true)
    public ScriptValidationResponse validateScript(Long projectId, String yamlContent) {
        loadProject(projectId);
        SchemaValidationResult validationResult = schemaValidateStep.execute(yamlContent);
        log.info(
            "【剧本版本】完成 YAML 校验，项目ID：{}，是否通过：{}，错误数：{}",
            projectId,
            validationResult.isValid(),
            validationResult.getErrors().size()
        );
        return ScriptValidationResponse.from(projectId, SCHEMA_VERSION, validationResult);
    }

    /**
     * 将当前 YAML 内容保存为新的人工编辑版本。
     *
     * @param projectId 项目 ID
     * @param title 新版本标题
     * @param yamlContent 用户编辑后的 YAML 原文
     * @return 保存后的新版本详情
     */
    @Override
    @Transactional
    public AdaptationScriptResponse saveScriptVersion(Long projectId, String title, String yamlContent) {
        return saveScriptVersion(projectId, title, yamlContent, SOURCE_TYPE_USER_EDITED);
    }

    /**
     * 将当前 YAML 内容保存为指定来源的新版本。
     *
     * @param projectId 项目 ID
     * @param title 新版本标题
     * @param yamlContent 用户编辑后的 YAML 原文
     * @param sourceType 版本来源类型
     * @return 保存后的新版本详情
     */
    @Override
    @Transactional
    public AdaptationScriptResponse saveScriptVersion(Long projectId, String title, String yamlContent, String sourceType) {
        Project project = loadProject(projectId);
        SchemaValidationResult validationResult = schemaValidateStep.execute(yamlContent);
        LocalDateTime now = LocalDateTime.now();

        ScriptVersion scriptVersion = new ScriptVersion();
        scriptVersion.setProjectId(projectId);
        scriptVersion.setVersionNo(resolveNextVersionNo(projectId));
        scriptVersion.setSourceType(resolveSourceType(sourceType));
        scriptVersion.setTitle(resolveEditedVersionTitle(project, title));
        scriptVersion.setCreatedAt(now);
        scriptVersion.setUpdatedAt(now);
        scriptVersionMapper.insert(scriptVersion);

        YamlSnapshot yamlSnapshot = new YamlSnapshot();
        yamlSnapshot.setProjectId(projectId);
        yamlSnapshot.setScriptVersionId(scriptVersion.getId());
        yamlSnapshot.setSchemaVersion(SCHEMA_VERSION);
        yamlSnapshot.setYamlContent(yamlContent);
        yamlSnapshot.setValidationStatus(validationResult.isValid() ? "PASSED" : "FAILED");
        yamlSnapshot.setValidationErrors(writeAsJson(validationResult.getErrors()));
        yamlSnapshot.setCreatedAt(now);
        yamlSnapshot.setUpdatedAt(now);
        yamlSnapshotMapper.insert(yamlSnapshot);

        log.info(
            "【剧本版本】保存人工编辑版本成功，项目ID：{}，版本号：{}，校验状态：{}",
            projectId,
            scriptVersion.getVersionNo(),
            yamlSnapshot.getValidationStatus()
        );
        return AdaptationScriptResponse.from(
            projectId,
            scriptVersion,
            yamlSnapshot,
            null,
            validationResult.getErrors().stream().map(ScriptValidationErrorResponse::from).toList()
        );
    }

    /**
     * 加载指定项目实体，若不存在则抛出异常。
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
     * 查询指定项目下全部剧本版本。
     *
     * @param projectId 项目 ID
     * @return 按版本号倒序排列的剧本版本列表
     */
    private List<ScriptVersion> loadScriptVersions(Long projectId) {
        return scriptVersionMapper.selectList(
            new LambdaQueryWrapper<ScriptVersion>()
                .eq(ScriptVersion::getProjectId, projectId)
                .orderByDesc(ScriptVersion::getVersionNo)
        );
    }

    /**
     * 加载指定项目下的某一个剧本版本。
     *
     * @param projectId 项目 ID
     * @param scriptVersionId 剧本版本 ID
     * @return 剧本版本实体
     */
    private ScriptVersion loadScriptVersion(Long projectId, Long scriptVersionId) {
        ScriptVersion scriptVersion = scriptVersionMapper.selectOne(
            new LambdaQueryWrapper<ScriptVersion>()
                .eq(ScriptVersion::getProjectId, projectId)
                .eq(ScriptVersion::getId, scriptVersionId)
                .last("limit 1")
        );
        if (scriptVersion == null) {
            throw new ResourceNotFoundException("当前项目下不存在指定的剧本版本。");
        }
        return scriptVersion;
    }

    /**
     * 加载指定版本对应的最新 YAML 快照。
     *
     * @param projectId 项目 ID
     * @param scriptVersionId 剧本版本 ID
     * @return 指定版本对应的最新 YAML 快照
     */
    private YamlSnapshot loadLatestYamlSnapshot(Long projectId, Long scriptVersionId) {
        YamlSnapshot yamlSnapshot = yamlSnapshotMapper.selectOne(
            new LambdaQueryWrapper<YamlSnapshot>()
                .eq(YamlSnapshot::getProjectId, projectId)
                .eq(YamlSnapshot::getScriptVersionId, scriptVersionId)
                .orderByDesc(YamlSnapshot::getId)
                .last("limit 1")
        );
        if (yamlSnapshot == null) {
            throw new ResourceNotFoundException("当前剧本版本缺少对应的 YAML 快照。");
        }
        return yamlSnapshot;
    }

    /**
     * 为剧本版本列表批量装配最新 YAML 快照映射。
     *
     * @param projectId 项目 ID
     * @param scriptVersions 当前项目下的剧本版本列表
     * @return 以剧本版本 ID 为键的最新 YAML 快照映射
     */
    private Map<Long, YamlSnapshot> buildLatestSnapshotMap(Long projectId, List<ScriptVersion> scriptVersions) {
        List<Long> scriptVersionIds = scriptVersions.stream().map(ScriptVersion::getId).toList();
        if (CollectionUtils.isEmpty(scriptVersionIds)) {
            return Collections.emptyMap();
        }

        List<YamlSnapshot> yamlSnapshots = yamlSnapshotMapper.selectList(
            new LambdaQueryWrapper<YamlSnapshot>()
                .eq(YamlSnapshot::getProjectId, projectId)
                .in(YamlSnapshot::getScriptVersionId, scriptVersionIds)
                .orderByDesc(YamlSnapshot::getId)
        );

        Map<Long, YamlSnapshot> latestSnapshotMap = new LinkedHashMap<>();
        for (YamlSnapshot yamlSnapshot : yamlSnapshots) {
            latestSnapshotMap.putIfAbsent(yamlSnapshot.getScriptVersionId(), yamlSnapshot);
        }
        return latestSnapshotMap;
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
            throw new IllegalStateException("剧本版本校验错误反序列化失败。", exception);
        }
    }

    /**
     * 计算当前项目下一个剧本版本号。
     *
     * @param projectId 项目 ID
     * @return 下一个剧本版本号
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
     * 生成人工编辑版本的标题。
     *
     * @param project 项目实体
     * @param title 用户传入的标题
     * @return 用于落库的版本标题
     */
    private String resolveEditedVersionTitle(Project project, String title) {
        if (StringUtils.hasText(title)) {
            return title.trim();
        }
        return project.getTitle() + " - 手动编辑稿";
    }

    /**
     * 规范化版本来源类型，避免写入空值。
     *
     * @param sourceType 调用方传入的版本来源
     * @return 可落库的版本来源
     */
    private String resolveSourceType(String sourceType) {
        if (StringUtils.hasText(sourceType)) {
            return sourceType.trim();
        }
        return SOURCE_TYPE_USER_EDITED;
    }

    /**
     * 将校验错误列表序列化为 JSON 字符串。
     *
     * @param value 待序列化对象
     * @return JSON 字符串
     */
    private String writeAsJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("剧本版本结果序列化失败。", exception);
        }
    }
}
