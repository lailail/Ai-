package com.qiniuyun.novelscript.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.qiniuyun.novelscript.pipeline.model.ScriptDocument;
import com.qiniuyun.novelscript.pipeline.model.ScriptEpisodeResult;
import com.qiniuyun.novelscript.pipeline.model.ScriptSceneDialogue;
import com.qiniuyun.novelscript.pipeline.model.ScriptSceneResult;
import com.qiniuyun.novelscript.pipeline.model.StoryBibleCharacter;
import com.qiniuyun.novelscript.service.ScreenplayRenderService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 基于固定模板的正式剧本渲染服务实现。
 */
@Service
public class ScreenplayRenderServiceImpl implements ScreenplayRenderService {

    private final YAMLMapper yamlMapper;

    /**
     * 构造正式剧本渲染服务实现。
     */
    public ScreenplayRenderServiceImpl() {
        this.yamlMapper = YAMLMapper.builder().findAndAddModules().build();
    }

    /**
     * 将 YAML 原文解析为结构化剧本文档。
     *
     * @param yamlContent YAML 原文
     * @return 结构化剧本文档
     */
    @Override
    public ScriptDocument parseScriptDocument(String yamlContent) {
        if (!StringUtils.hasText(yamlContent)) {
            throw new IllegalArgumentException("正式剧本渲染时，YAML 内容不能为空。");
        }

        try {
            return yamlMapper.readValue(yamlContent, ScriptDocument.class);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("正式剧本渲染时，YAML 解析失败。", exception);
        }
    }

    /**
     * 将结构化剧本文档渲染为正式剧本 Markdown。
     *
     * @param scriptDocument 结构化剧本文档
     * @return 正式剧本 Markdown
     */
    @Override
    public String renderMarkdown(ScriptDocument scriptDocument) {
        if (scriptDocument == null) {
            throw new IllegalArgumentException("正式剧本渲染时，剧本文档不能为空。");
        }

        StringBuilder builder = new StringBuilder();
        Map<String, String> characterNameMap = buildCharacterNameMap(scriptDocument);

        appendDocumentHeader(builder, scriptDocument);
        appendEpisodes(builder, scriptDocument.getEpisodes(), characterNameMap);
        return builder.toString().trim();
    }

    /**
     * 追加剧本文档头部信息。
     *
     * @param builder 文本构建器
     * @param scriptDocument 结构化剧本文档
     */
    private void appendDocumentHeader(StringBuilder builder, ScriptDocument scriptDocument) {
        String projectTitle = scriptDocument.getProject() == null ? "" : safeText(scriptDocument.getProject().getTitle());
        String documentTitle = StringUtils.hasText(projectTitle) ? projectTitle : "未命名项目";
        builder.append("# ").append(documentTitle).append(" 正式剧本").append("\n\n");
    }

    /**
     * 逐集追加正式剧本内容。
     *
     * @param builder 文本构建器
     * @param episodes 剧集列表
     * @param characterNameMap 角色名称映射
     */
    private void appendEpisodes(
        StringBuilder builder,
        List<ScriptEpisodeResult> episodes,
        Map<String, String> characterNameMap
    ) {
        if (CollectionUtils.isEmpty(episodes)) {
            builder.append("暂无可展示的剧本内容。").append("\n");
            return;
        }

        for (int episodeIndex = 0; episodeIndex < episodes.size(); episodeIndex++) {
            ScriptEpisodeResult episode = episodes.get(episodeIndex);
            builder.append("## 第").append(episodeIndex + 1).append("集：")
                .append(safeText(episode.getTitle(), "未命名剧集"))
                .append("\n\n");

            if (StringUtils.hasText(episode.getPremise())) {
                builder.append("> 剧集前提：").append(episode.getPremise().trim()).append("\n\n");
            }

            appendScenes(builder, episode.getScenes(), characterNameMap);
        }
    }

    /**
     * 逐场追加正式剧本内容。
     *
     * @param builder 文本构建器
     * @param scenes 场景列表
     * @param characterNameMap 角色名称映射
     */
    private void appendScenes(
        StringBuilder builder,
        List<ScriptSceneResult> scenes,
        Map<String, String> characterNameMap
    ) {
        if (CollectionUtils.isEmpty(scenes)) {
            builder.append("（当前剧集暂无场景内容）").append("\n\n");
            return;
        }

        for (int sceneIndex = 0; sceneIndex < scenes.size(); sceneIndex++) {
            ScriptSceneResult scene = scenes.get(sceneIndex);
            builder.append("### 场 ").append(sceneIndex + 1).append("\n");
            builder.append(safeText(scene.getSlugline(), "未命名场景")).append("\n\n");

            if (StringUtils.hasText(scene.getPurpose())) {
                builder.append("目的：").append(scene.getPurpose().trim()).append("\n\n");
            }

            appendActions(builder, scene.getActions());
            appendDialogue(builder, scene.getDialogue(), characterNameMap);

            if (StringUtils.hasText(scene.getTransition())) {
                builder.append(scene.getTransition().trim()).append("\n\n");
            }
        }
    }

    /**
     * 追加动作段落。
     *
     * @param builder 文本构建器
     * @param actions 动作列表
     */
    private void appendActions(StringBuilder builder, List<String> actions) {
        if (CollectionUtils.isEmpty(actions)) {
            return;
        }

        for (String action : actions) {
            if (StringUtils.hasText(action)) {
                builder.append(action.trim()).append("\n\n");
            }
        }
    }

    /**
     * 追加对白段落。
     *
     * @param builder 文本构建器
     * @param dialogues 对白列表
     * @param characterNameMap 角色名称映射
     */
    private void appendDialogue(
        StringBuilder builder,
        List<ScriptSceneDialogue> dialogues,
        Map<String, String> characterNameMap
    ) {
        if (CollectionUtils.isEmpty(dialogues)) {
            return;
        }

        for (ScriptSceneDialogue dialogue : dialogues) {
            if (dialogue == null || !StringUtils.hasText(dialogue.getLine())) {
                continue;
            }

            builder.append(resolveCharacterName(dialogue.getCharacterId(), characterNameMap)).append("\n");
            if (StringUtils.hasText(dialogue.getParenthetical())) {
                builder.append("（").append(dialogue.getParenthetical().trim()).append("）").append("\n");
            }
            builder.append(dialogue.getLine().trim()).append("\n");
            if (StringUtils.hasText(dialogue.getSubtext())) {
                builder.append("潜台词：").append(dialogue.getSubtext().trim()).append("\n");
            }
            builder.append("\n");
        }
    }

    /**
     * 构建角色 ID 到角色名的映射。
     *
     * @param scriptDocument 结构化剧本文档
     * @return 角色名称映射
     */
    private Map<String, String> buildCharacterNameMap(ScriptDocument scriptDocument) {
        Map<String, String> characterNameMap = new LinkedHashMap<>();
        if (scriptDocument.getStoryBible() == null || CollectionUtils.isEmpty(scriptDocument.getStoryBible().getCharacters())) {
            return characterNameMap;
        }

        for (StoryBibleCharacter character : scriptDocument.getStoryBible().getCharacters()) {
            if (character != null && StringUtils.hasText(character.getId()) && StringUtils.hasText(character.getName())) {
                characterNameMap.put(character.getId(), character.getName());
            }
        }
        return characterNameMap;
    }

    /**
     * 解析对白角色名称。
     *
     * @param characterId 角色 ID
     * @param characterNameMap 角色名称映射
     * @return 角色名称
     */
    private String resolveCharacterName(String characterId, Map<String, String> characterNameMap) {
        if (!StringUtils.hasText(characterId)) {
            return "角色";
        }
        return characterNameMap.getOrDefault(characterId, characterId);
    }

    /**
     * 返回安全的文本内容。
     *
     * @param value 原始文本
     * @return 处理后的文本
     */
    private String safeText(String value) {
        return safeText(value, "");
    }

    /**
     * 返回带默认值的安全文本内容。
     *
     * @param value 原始文本
     * @param defaultValue 默认值
     * @return 处理后的文本
     */
    private String safeText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }
}
