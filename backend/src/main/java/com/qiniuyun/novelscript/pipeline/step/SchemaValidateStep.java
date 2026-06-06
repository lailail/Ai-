package com.qiniuyun.novelscript.pipeline.step;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.qiniuyun.novelscript.pipeline.model.SchemaValidationError;
import com.qiniuyun.novelscript.pipeline.model.SchemaValidationResult;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 负责校验 YAML 是否符合项目 Schema 要求。
 */
@Component
public class SchemaValidateStep {

    private final YAMLMapper yamlMapper;

    /**
     * 构造 YAML Schema 校验步骤。
     */
    public SchemaValidateStep() {
        this.yamlMapper = YAMLMapper.builder().findAndAddModules().build();
    }

    /**
     * 校验 YAML 文本。
     *
     * @param yamlContent YAML 文本
     * @return 结构化校验结果
     */
    public SchemaValidationResult execute(String yamlContent) {
        SchemaValidationResult result = new SchemaValidationResult();
        if (!StringUtils.hasText(yamlContent)) {
            result.setValid(false);
            result.getErrors().add(new SchemaValidationError("yaml", "YAML 内容不能为空。"));
            return result;
        }

        JsonNode rootNode;
        try {
            rootNode = yamlMapper.readTree(yamlContent);
        }
        catch (IOException exception) {
            result.setValid(false);
            result.getErrors().add(new SchemaValidationError("yaml", "YAML 解析失败。"));
            return result;
        }

        validateRequiredText(rootNode, "schema_version", result);
        validateProject(rootNode.path("project"), result);
        validateStoryBible(rootNode.path("story_bible"), result);
        validateEpisodes(
            rootNode.path("episodes"),
            rootNode.path("project").path("source_chapters"),
            rootNode.path("story_bible").path("characters"),
            result
        );
        validateMetadata(rootNode.path("metadata"), result);
        result.setValid(result.getErrors().isEmpty());
        return result;
    }

    /**
     * 校验项目顶层节点。
     *
     * @param projectNode 项目节点
     * @param result 校验结果
     */
    private void validateProject(JsonNode projectNode, SchemaValidationResult result) {
        if (projectNode.isMissingNode() || projectNode.isNull()) {
            result.getErrors().add(new SchemaValidationError("project", "project 为必填字段。"));
            return;
        }
        validateRequiredText(projectNode, "title", result, "project.title");
    }

    /**
     * 校验 Story Bible 顶层节点。
     *
     * @param storyBibleNode Story Bible 节点
     * @param result 校验结果
     */
    private void validateStoryBible(JsonNode storyBibleNode, SchemaValidationResult result) {
        if (storyBibleNode.isMissingNode() || storyBibleNode.isNull()) {
            result.getErrors().add(new SchemaValidationError("story_bible", "story_bible 为必填字段。"));
        }
    }

    /**
     * 校验剧集和场景结构。
     *
     * @param episodesNode 剧集节点
     * @param sourceChaptersNode 原始章节节点
     * @param storyCharactersNode Story Bible 角色节点
     * @param result 校验结果
     */
    private void validateEpisodes(
        JsonNode episodesNode,
        JsonNode sourceChaptersNode,
        JsonNode storyCharactersNode,
        SchemaValidationResult result
    ) {
        if (!episodesNode.isArray()) {
            result.getErrors().add(new SchemaValidationError("episodes", "episodes 必须是数组。"));
            return;
        }

        Set<String> validCharacterIds = collectCharacterIds(storyCharactersNode);
        Set<String> validChapterRefs = collectChapterRefs(sourceChaptersNode);
        for (int episodeIndex = 0; episodeIndex < episodesNode.size(); episodeIndex++) {
            JsonNode episodeNode = episodesNode.get(episodeIndex);
            String episodePath = "episodes[" + episodeIndex + "]";
            validateRequiredText(episodeNode, "id", result, episodePath + ".id");

            JsonNode scenesNode = episodeNode.path("scenes");
            if (!scenesNode.isArray()) {
                result.getErrors().add(new SchemaValidationError(episodePath + ".scenes", "scenes 必须是数组。"));
                continue;
            }

            for (int sceneIndex = 0; sceneIndex < scenesNode.size(); sceneIndex++) {
                JsonNode sceneNode = scenesNode.get(sceneIndex);
                String scenePath = episodePath + ".scenes[" + sceneIndex + "]";
                validateRequiredText(sceneNode, "id", result, scenePath + ".id");
                validateRequiredText(sceneNode, "slugline", result, scenePath + ".slugline");
                validateSourceRefs(sceneNode.path("source_refs"), validChapterRefs, scenePath + ".source_refs", result);
                validateCharacters(sceneNode.path("characters"), validCharacterIds, scenePath + ".characters", result);
                validateDialogue(sceneNode.path("dialogue"), validCharacterIds, scenePath + ".dialogue", result);
            }
        }
    }

    /**
     * 校验元数据节点。
     *
     * @param metadataNode 元数据节点
     * @param result 校验结果
     */
    private void validateMetadata(JsonNode metadataNode, SchemaValidationResult result) {
        if (metadataNode.isMissingNode() || metadataNode.isNull()) {
            result.getErrors().add(new SchemaValidationError("metadata", "metadata 为必填字段。"));
            return;
        }
        validateRequiredText(metadataNode, "generator", result, "metadata.generator");
    }

    /**
     * 校验来源章节引用是否合法。
     *
     * @param sourceRefsNode 来源引用节点
     * @param validChapterRefs 合法章节引用集合
     * @param path 当前错误路径
     * @param result 校验结果
     */
    private void validateSourceRefs(
        JsonNode sourceRefsNode,
        Set<String> validChapterRefs,
        String path,
        SchemaValidationResult result
    ) {
        if (!sourceRefsNode.isArray() || sourceRefsNode.isEmpty()) {
            result.getErrors().add(new SchemaValidationError(path, "source_refs 为必填数组。"));
            return;
        }

        for (JsonNode sourceRefNode : sourceRefsNode) {
            String sourceRef = sourceRefNode.asText();
            if (!validChapterRefs.isEmpty() && !validChapterRefs.contains(sourceRef)) {
                result.getErrors().add(new SchemaValidationError(path, "source_refs 中存在未录入的章节引用：" + sourceRef));
            }
        }
    }

    /**
     * 校验场景角色引用是否合法。
     *
     * @param charactersNode 角色引用节点
     * @param validCharacterIds 合法角色 ID 集合
     * @param path 当前错误路径
     * @param result 校验结果
     */
    private void validateCharacters(
        JsonNode charactersNode,
        Set<String> validCharacterIds,
        String path,
        SchemaValidationResult result
    ) {
        if (!charactersNode.isArray()) {
            return;
        }

        for (JsonNode characterNode : charactersNode) {
            String characterId = characterNode.asText();
            if (StringUtils.hasText(characterId) && !validCharacterIds.isEmpty() && !validCharacterIds.contains(characterId)) {
                result.getErrors().add(new SchemaValidationError(path, "characters 中存在未定义角色：" + characterId));
            }
        }
    }

    /**
     * 校验对白结构及角色引用是否合法。
     *
     * @param dialogueNode 对白节点
     * @param validCharacterIds 合法角色 ID 集合
     * @param path 当前错误路径
     * @param result 校验结果
     */
    private void validateDialogue(
        JsonNode dialogueNode,
        Set<String> validCharacterIds,
        String path,
        SchemaValidationResult result
    ) {
        if (!dialogueNode.isArray()) {
            return;
        }

        for (int dialogueIndex = 0; dialogueIndex < dialogueNode.size(); dialogueIndex++) {
            JsonNode dialogueItemNode = dialogueNode.get(dialogueIndex);
            String itemPath = path + "[" + dialogueIndex + "]";
            String characterId = dialogueItemNode.path("character_id").asText();
            if (StringUtils.hasText(characterId) && !validCharacterIds.isEmpty() && !validCharacterIds.contains(characterId)) {
                result.getErrors().add(new SchemaValidationError(itemPath + ".character_id", "dialogue.character_id 未在 story_bible.characters 中定义。"));
            }
            validateRequiredText(dialogueItemNode, "line", result, itemPath + ".line");
        }
    }

    /**
     * 收集 Story Bible 中定义的角色 ID。
     *
     * @param storyCharactersNode Story Bible 角色节点
     * @return 合法角色 ID 集合
     */
    private Set<String> collectCharacterIds(JsonNode storyCharactersNode) {
        Set<String> characterIds = new HashSet<>();
        if (!storyCharactersNode.isArray()) {
            return characterIds;
        }

        for (JsonNode characterNode : storyCharactersNode) {
            String characterId = characterNode.path("id").asText();
            if (StringUtils.hasText(characterId)) {
                characterIds.add(characterId);
            }
        }
        return characterIds;
    }

    /**
     * 根据项目章节号收集合法的章节引用格式。
     *
     * @param sourceChaptersNode 原始章节节点
     * @return 合法章节引用集合
     */
    private Set<String> collectChapterRefs(JsonNode sourceChaptersNode) {
        Set<String> chapterRefs = new HashSet<>();
        if (!sourceChaptersNode.isArray()) {
            return chapterRefs;
        }

        for (JsonNode chapterNode : sourceChaptersNode) {
            chapterRefs.add("chapter:" + chapterNode.asInt());
        }
        return chapterRefs;
    }

    /**
     * 校验默认路径下的必填文本字段。
     *
     * @param parentNode 父节点
     * @param fieldName 字段名
     * @param result 校验结果
     */
    private void validateRequiredText(JsonNode parentNode, String fieldName, SchemaValidationResult result) {
        validateRequiredText(parentNode, fieldName, result, fieldName);
    }

    /**
     * 校验指定路径下的必填文本字段。
     *
     * @param parentNode 父节点
     * @param fieldName 字段名
     * @param result 校验结果
     * @param path 当前错误路径
     */
    private void validateRequiredText(
        JsonNode parentNode,
        String fieldName,
        SchemaValidationResult result,
        String path
    ) {
        JsonNode fieldNode = parentNode.path(fieldName);
        if (!fieldNode.isTextual() || !StringUtils.hasText(fieldNode.asText())) {
            result.getErrors().add(new SchemaValidationError(path, path + " 为必填字段。"));
        }
    }
}
