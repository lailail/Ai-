package com.qiniuyun.novelscript.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiniuyun.novelscript.common.exception.ResourceNotFoundException;
import com.qiniuyun.novelscript.controller.response.AdaptationScriptResponse;
import com.qiniuyun.novelscript.controller.response.ScreenplayResponse;
import com.qiniuyun.novelscript.domain.entity.Project;
import com.qiniuyun.novelscript.domain.entity.ScreenplaySnapshot;
import com.qiniuyun.novelscript.domain.entity.ScriptVersion;
import com.qiniuyun.novelscript.domain.entity.YamlSnapshot;
import com.qiniuyun.novelscript.mapper.ProjectMapper;
import com.qiniuyun.novelscript.mapper.ScreenplaySnapshotMapper;
import com.qiniuyun.novelscript.mapper.ScriptVersionMapper;
import com.qiniuyun.novelscript.mapper.YamlSnapshotMapper;
import com.qiniuyun.novelscript.pipeline.model.ScriptDocument;
import com.qiniuyun.novelscript.pipeline.model.ScriptEpisodeResult;
import com.qiniuyun.novelscript.pipeline.model.ScriptSceneDialogue;
import com.qiniuyun.novelscript.pipeline.model.ScriptSceneResult;
import com.qiniuyun.novelscript.pipeline.model.StoryBibleCharacter;
import com.qiniuyun.novelscript.pipeline.step.YamlSerializeStep;
import com.qiniuyun.novelscript.service.ScreenplayRenderService;
import com.qiniuyun.novelscript.service.ScreenplayService;
import com.qiniuyun.novelscript.service.ScriptVersionService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 负责正式剧本查询、渲染、导出与回写 YAML 的服务实现。
 */
@Service
public class ScreenplayServiceImpl implements ScreenplayService {

    private static final String RENDER_VERSION = "v1";
    private static final String SOURCE_TYPE_USER_EDITED = "USER_EDITED";
    private static final Pattern EPISODE_TITLE_PATTERN = Pattern.compile("^##\\s*第[^：:]+[：:]\\s*(.+)$");
    private static final Pattern SCENE_HEADING_PATTERN = Pattern.compile("^###\\s*场\\s*\\d+.*$");
    private static final Pattern PARENTHETICAL_PATTERN = Pattern.compile("^[（(](.+)[）)]$");
    private static final Pattern TRANSITION_PATTERN = Pattern.compile("^[A-Z][A-Z_0-9]*$");

    private final ProjectMapper projectMapper;
    private final ScriptVersionMapper scriptVersionMapper;
    private final YamlSnapshotMapper yamlSnapshotMapper;
    private final ScreenplaySnapshotMapper screenplaySnapshotMapper;
    private final ScreenplayRenderService screenplayRenderService;
    private final ScriptVersionService scriptVersionService;
    private final YamlSerializeStep yamlSerializeStep;

    /**
     * 构造正式剧本服务实现。
     *
     * @param projectMapper 项目 Mapper
     * @param scriptVersionMapper 剧本版本 Mapper
     * @param yamlSnapshotMapper YAML 快照 Mapper
     * @param screenplaySnapshotMapper 正式剧本快照 Mapper
     * @param screenplayRenderService 正式剧本渲染服务
     * @param scriptVersionService 剧本版本服务
     * @param yamlSerializeStep YAML 序列化步骤
     */
    public ScreenplayServiceImpl(
        ProjectMapper projectMapper,
        ScriptVersionMapper scriptVersionMapper,
        YamlSnapshotMapper yamlSnapshotMapper,
        ScreenplaySnapshotMapper screenplaySnapshotMapper,
        ScreenplayRenderService screenplayRenderService,
        ScriptVersionService scriptVersionService,
        YamlSerializeStep yamlSerializeStep
    ) {
        this.projectMapper = projectMapper;
        this.scriptVersionMapper = scriptVersionMapper;
        this.yamlSnapshotMapper = yamlSnapshotMapper;
        this.screenplaySnapshotMapper = screenplaySnapshotMapper;
        this.screenplayRenderService = screenplayRenderService;
        this.scriptVersionService = scriptVersionService;
        this.yamlSerializeStep = yamlSerializeStep;
    }

    /**
     * 主动渲染指定版本的正式剧本，并保存为新快照。
     *
     * @param projectId 项目 ID
     * @param scriptVersionId 剧本版本 ID
     * @return 渲染后的正式剧本响应
     */
    @Override
    @Transactional
    public ScreenplayResponse renderScreenplay(Long projectId, Long scriptVersionId) {
        loadProject(projectId);
        ScriptVersion scriptVersion = loadScriptVersion(projectId, scriptVersionId);
        YamlSnapshot yamlSnapshot = loadLatestYamlSnapshot(projectId, scriptVersionId);
        ScreenplaySnapshot screenplaySnapshot = renderAndPersist(projectId, scriptVersion, yamlSnapshot);
        return ScreenplayResponse.from(projectId, scriptVersion, screenplaySnapshot);
    }

    /**
     * 查询当前项目最新版本对应的正式剧本。
     *
     * @param projectId 项目 ID
     * @return 最新正式剧本响应
     */
    @Override
    @Transactional
    public ScreenplayResponse getLatestScreenplay(Long projectId) {
        loadProject(projectId);
        ScriptVersion latestScriptVersion = scriptVersionMapper.selectOne(
            new LambdaQueryWrapper<ScriptVersion>()
                .eq(ScriptVersion::getProjectId, projectId)
                .orderByDesc(ScriptVersion::getVersionNo)
                .last("limit 1")
        );
        if (latestScriptVersion == null) {
            throw new ResourceNotFoundException("当前项目还没有生成剧本版本。");
        }
        return getScreenplay(projectId, latestScriptVersion.getId());
    }

    /**
     * 查询指定剧本版本对应的正式剧本。
     *
     * @param projectId 项目 ID
     * @param scriptVersionId 剧本版本 ID
     * @return 指定版本正式剧本响应
     */
    @Override
    @Transactional
    public ScreenplayResponse getScreenplay(Long projectId, Long scriptVersionId) {
        loadProject(projectId);
        ScriptVersion scriptVersion = loadScriptVersion(projectId, scriptVersionId);
        ScreenplaySnapshot latestSnapshot = loadLatestScreenplaySnapshot(projectId, scriptVersionId);
        if (latestSnapshot != null) {
            return ScreenplayResponse.from(projectId, scriptVersion, latestSnapshot);
        }

        YamlSnapshot yamlSnapshot = loadLatestYamlSnapshot(projectId, scriptVersionId);
        ScreenplaySnapshot screenplaySnapshot = renderAndPersist(projectId, scriptVersion, yamlSnapshot);
        return ScreenplayResponse.from(projectId, scriptVersion, screenplaySnapshot);
    }

    /**
     * 将正式剧本编辑结果同步回 YAML，并生成新的剧本版本。
     *
     * @param projectId 项目 ID
     * @param scriptVersionId 原始剧本版本 ID
     * @param title 新版本标题
     * @param markdownContent 编辑后的正式剧本 Markdown
     * @return 新生成的 YAML 版本响应
     */
    @Override
    @Transactional
    public AdaptationScriptResponse syncScreenplayToYaml(
        Long projectId,
        Long scriptVersionId,
        String title,
        String markdownContent
    ) {
        loadProject(projectId);
        loadScriptVersion(projectId, scriptVersionId);
        YamlSnapshot yamlSnapshot = loadLatestYamlSnapshot(projectId, scriptVersionId);

        ScriptDocument scriptDocument = screenplayRenderService.parseScriptDocument(yamlSnapshot.getYamlContent());
        applyEditableScreenplay(scriptDocument, markdownContent);
        String updatedYaml = yamlSerializeStep.execute(scriptDocument);

        AdaptationScriptResponse response = scriptVersionService.saveScriptVersion(
            projectId,
            title,
            updatedYaml,
            SOURCE_TYPE_USER_EDITED
        );
        persistScreenplaySnapshot(
            projectId,
            response.getScriptVersionId(),
            response.getTitle(),
            markdownContent,
            response.getSourceType()
        );
        return response;
    }

    /**
     * 保存正式剧本编辑结果。
     *
     * @param projectId 项目 ID
     * @param scriptVersionId 原始剧本版本 ID
     * @param title 新版本标题
     * @param markdownContent 编辑后的正式剧本 Markdown
     * @return 新生成的 YAML 版本响应
     */
    @Override
    @Transactional
    public AdaptationScriptResponse saveScreenplay(Long projectId, Long scriptVersionId, String title, String markdownContent) {
        return syncScreenplayToYaml(projectId, scriptVersionId, title, markdownContent);
    }

    /**
     * 导出指定剧本版本的 Markdown 正文。
     *
     * @param projectId 项目 ID
     * @param scriptVersionId 剧本版本 ID
     * @return Markdown 文本
     */
    @Override
    @Transactional
    public String exportMarkdown(Long projectId, Long scriptVersionId) {
        return getScreenplay(projectId, scriptVersionId).getMarkdownContent();
    }

    /**
     * 导出指定剧本版本的纯文本内容。
     *
     * @param projectId 项目 ID
     * @param scriptVersionId 剧本版本 ID
     * @return 纯文本内容
     */
    @Override
    @Transactional
    public String exportPlainText(Long projectId, Long scriptVersionId) {
        String markdownContent = getScreenplay(projectId, scriptVersionId).getMarkdownContent();
        return markdownContent
            .replace("### ", "")
            .replace("## ", "")
            .replace("# ", "")
            .replace("> ", "")
            .replace("**", "");
    }

    /**
     * 将正式剧本文本中的可编辑字段应用回结构化剧本文档。
     *
     * @param scriptDocument 结构化剧本文档
     * @param markdownContent 用户编辑后的正式剧本文本
     */
    private void applyEditableScreenplay(ScriptDocument scriptDocument, String markdownContent) {
        if (scriptDocument == null) {
            throw new IllegalArgumentException("正式剧本回写时，剧本文档不能为空。");
        }
        if (!StringUtils.hasText(markdownContent)) {
            throw new IllegalArgumentException("正式剧本回写时，剧本文本不能为空。");
        }

        EditableScreenplay editableScreenplay = parseEditableScreenplay(scriptDocument, markdownContent);
        validateEditableStructure(scriptDocument, editableScreenplay);
        mergeEditableScreenplay(scriptDocument, editableScreenplay);
    }

    /**
     * 解析正式剧本文本中的可编辑结构。
     *
     * @param scriptDocument 原始结构化剧本文档
     * @param markdownContent 用户编辑后的正式剧本文本
     * @return 解析后的可编辑结构
     */
    private EditableScreenplay parseEditableScreenplay(ScriptDocument scriptDocument, String markdownContent) {
        EditableScreenplay editableScreenplay = new EditableScreenplay();
        Map<String, String> characterNameMap = buildCharacterNameMap(scriptDocument);
        List<String> lines = splitLines(markdownContent);

        EditableEpisode currentEpisode = null;
        EditableScene currentScene = null;
        int lineIndex = 0;
        while (lineIndex < lines.size()) {
            String line = lines.get(lineIndex).trim();
            if (!StringUtils.hasText(line) || line.startsWith("# ")) {
                lineIndex++;
                continue;
            }

            String episodeTitle = extractEpisodeTitle(line);
            if (episodeTitle != null) {
                currentEpisode = new EditableEpisode();
                currentEpisode.setTitle(episodeTitle);
                editableScreenplay.getEpisodes().add(currentEpisode);
                currentScene = null;
                lineIndex++;
                continue;
            }

            if (line.startsWith("> 剧集前提：")) {
                lineIndex++;
                continue;
            }

            if (SCENE_HEADING_PATTERN.matcher(line).matches()) {
                if (currentEpisode == null) {
                    throw new IllegalArgumentException("正式剧本格式不正确，存在未归属剧集的场次标题。");
                }
                currentScene = new EditableScene();
                currentEpisode.getScenes().add(currentScene);
                lineIndex++;
                continue;
            }

            if (currentScene == null) {
                lineIndex++;
                continue;
            }

            if (line.startsWith("目的：")) {
                lineIndex++;
                continue;
            }

            if (!StringUtils.hasText(currentScene.getSlugline()) && looksLikeSlugline(line)) {
                currentScene.setSlugline(line);
                lineIndex++;
                continue;
            }

            if (TRANSITION_PATTERN.matcher(line).matches()) {
                currentScene.setTransition(line);
                lineIndex++;
                continue;
            }

            if (characterNameMap.containsKey(line)) {
                lineIndex = parseDialogueBlock(lines, lineIndex, currentScene, characterNameMap);
                continue;
            }

            currentScene.getActions().add(line);
            lineIndex++;
        }
        return editableScreenplay;
    }

    /**
     * 解析单个对话块，并推进游标位置。
     *
     * @param lines 全部剧本文本行
     * @param startIndex 当前开始行
     * @param scene 当前场景
     * @param characterNameMap 角色名到角色 ID 的映射
     * @return 解析完成后的下一行索引
     */
    private int parseDialogueBlock(
        List<String> lines,
        int startIndex,
        EditableScene scene,
        Map<String, String> characterNameMap
    ) {
        String characterName = lines.get(startIndex).trim();
        int currentIndex = startIndex + 1;

        while (currentIndex < lines.size() && !StringUtils.hasText(lines.get(currentIndex).trim())) {
            currentIndex++;
        }
        if (currentIndex >= lines.size()) {
            throw new IllegalArgumentException("正式剧本中的角色对白缺少台词内容。");
        }

        EditableDialogue dialogue = new EditableDialogue();
        dialogue.setCharacterId(characterNameMap.get(characterName));

        String line = lines.get(currentIndex).trim();
        Matcher parentheticalMatcher = PARENTHETICAL_PATTERN.matcher(line);
        if (parentheticalMatcher.matches()) {
            dialogue.setParenthetical(parentheticalMatcher.group(1).trim());
            currentIndex++;
            while (currentIndex < lines.size() && !StringUtils.hasText(lines.get(currentIndex).trim())) {
                currentIndex++;
            }
            if (currentIndex >= lines.size()) {
                throw new IllegalArgumentException("正式剧本中的角色对白缺少台词内容。");
            }
            line = lines.get(currentIndex).trim();
        }

        if (isStructureLine(line)) {
            throw new IllegalArgumentException("正式剧本中的角色对白缺少有效台词内容。");
        }

        dialogue.setLine(line);
        currentIndex++;
        while (currentIndex < lines.size() && !StringUtils.hasText(lines.get(currentIndex).trim())) {
            currentIndex++;
        }
        if (currentIndex < lines.size()) {
            String subtextLine = lines.get(currentIndex).trim();
            if (subtextLine.startsWith("潜台词：")) {
                dialogue.setSubtext(subtextLine.substring("潜台词：".length()).trim());
                currentIndex++;
            }
        }

        scene.getDialogues().add(dialogue);
        return currentIndex;
    }

    /**
     * 校验解析后的正式剧本结构是否与原始 YAML 结构一致。
     *
     * @param scriptDocument 原始结构化剧本文档
     * @param editableScreenplay 解析后的可编辑结构
     */
    private void validateEditableStructure(ScriptDocument scriptDocument, EditableScreenplay editableScreenplay) {
        if (CollectionUtils.isEmpty(scriptDocument.getEpisodes())) {
            throw new IllegalArgumentException("当前 YAML 中不存在可回写的剧集结构。");
        }
        if (editableScreenplay.getEpisodes().size() != scriptDocument.getEpisodes().size()) {
            throw new IllegalArgumentException("正式剧本中的剧集数量与原始 YAML 不一致，暂不支持跨结构改写。");
        }

        for (int episodeIndex = 0; episodeIndex < scriptDocument.getEpisodes().size(); episodeIndex++) {
            ScriptEpisodeResult scriptEpisode = scriptDocument.getEpisodes().get(episodeIndex);
            EditableEpisode editableEpisode = editableScreenplay.getEpisodes().get(episodeIndex);
            List<ScriptSceneResult> scriptScenes = scriptEpisode.getScenes() == null ? List.of() : scriptEpisode.getScenes();
            if (editableEpisode.getScenes().size() != scriptScenes.size()) {
                throw new IllegalArgumentException("正式剧本中的场次数量与原始 YAML 不一致，暂不支持增删场次。");
            }
        }
    }

    /**
     * 将解析出的正式剧本内容合并回结构化剧本文档。
     *
     * @param scriptDocument 原始结构化剧本文档
     * @param editableScreenplay 解析后的可编辑结构
     */
    private void mergeEditableScreenplay(ScriptDocument scriptDocument, EditableScreenplay editableScreenplay) {
        for (int episodeIndex = 0; episodeIndex < editableScreenplay.getEpisodes().size(); episodeIndex++) {
            EditableEpisode editableEpisode = editableScreenplay.getEpisodes().get(episodeIndex);
            ScriptEpisodeResult scriptEpisode = scriptDocument.getEpisodes().get(episodeIndex);

            if (StringUtils.hasText(editableEpisode.getTitle())) {
                scriptEpisode.setTitle(editableEpisode.getTitle());
            }

            for (int sceneIndex = 0; sceneIndex < editableEpisode.getScenes().size(); sceneIndex++) {
                EditableScene editableScene = editableEpisode.getScenes().get(sceneIndex);
                ScriptSceneResult scriptScene = scriptEpisode.getScenes().get(sceneIndex);

                if (StringUtils.hasText(editableScene.getSlugline())) {
                    scriptScene.setSlugline(editableScene.getSlugline());
                }
                scriptScene.setActions(new ArrayList<>(editableScene.getActions()));
                scriptScene.setDialogue(convertDialogues(editableScene.getDialogues()));
                scriptScene.setTransition(editableScene.getTransition());
            }
        }
    }

    /**
     * 将可编辑对白结构转换回 YAML 场景对白结构。
     *
     * @param dialogues 解析后的对白列表
     * @return 可直接写回 YAML 的对白列表
     */
    private List<ScriptSceneDialogue> convertDialogues(List<EditableDialogue> dialogues) {
        List<ScriptSceneDialogue> results = new ArrayList<>();
        for (EditableDialogue dialogue : dialogues) {
            ScriptSceneDialogue scriptSceneDialogue = new ScriptSceneDialogue();
            scriptSceneDialogue.setCharacterId(dialogue.getCharacterId());
            scriptSceneDialogue.setParenthetical(dialogue.getParenthetical());
            scriptSceneDialogue.setLine(dialogue.getLine());
            scriptSceneDialogue.setSubtext(dialogue.getSubtext());
            results.add(scriptSceneDialogue);
        }
        return results;
    }

    /**
     * 渲染并保存正式剧本快照。
     *
     * @param projectId 项目 ID
     * @param scriptVersion 剧本版本实体
     * @param yamlSnapshot YAML 快照实体
     * @return 新生成的正式剧本快照
     */
    private ScreenplaySnapshot renderAndPersist(Long projectId, ScriptVersion scriptVersion, YamlSnapshot yamlSnapshot) {
        ScriptDocument scriptDocument = screenplayRenderService.parseScriptDocument(yamlSnapshot.getYamlContent());
        String markdownContent = screenplayRenderService.renderMarkdown(scriptDocument);
        return persistScreenplaySnapshot(
            projectId,
            scriptVersion.getId(),
            resolveSnapshotTitle(scriptVersion),
            markdownContent,
            scriptVersion.getSourceType()
        );
    }

    /**
     * 将正式剧本快照持久化到数据库。
     *
     * @param projectId 项目 ID
     * @param scriptVersionId 剧本版本 ID
     * @param title 快照标题
     * @param markdownContent 正式剧本 Markdown 内容
     * @param sourceType 快照来源类型
     * @return 已保存的正式剧本快照
     */
    private ScreenplaySnapshot persistScreenplaySnapshot(
        Long projectId,
        Long scriptVersionId,
        String title,
        String markdownContent,
        String sourceType
    ) {
        LocalDateTime now = LocalDateTime.now();
        ScreenplaySnapshot screenplaySnapshot = new ScreenplaySnapshot();
        screenplaySnapshot.setProjectId(projectId);
        screenplaySnapshot.setScriptVersionId(scriptVersionId);
        screenplaySnapshot.setTitle(title);
        screenplaySnapshot.setMarkdownContent(markdownContent);
        screenplaySnapshot.setRenderVersion(RENDER_VERSION);
        screenplaySnapshot.setSourceType(sourceType);
        screenplaySnapshot.setCreatedAt(now);
        screenplaySnapshot.setUpdatedAt(now);
        screenplaySnapshotMapper.insert(screenplaySnapshot);
        return screenplaySnapshot;
    }

    /**
     * 读取项目实体，若不存在则抛出异常。
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
     * 读取指定项目下的剧本版本。
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
     * 读取指定剧本版本的最新 YAML 快照。
     *
     * @param projectId 项目 ID
     * @param scriptVersionId 剧本版本 ID
     * @return YAML 快照实体
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
     * 读取指定剧本版本的最新正式剧本快照。
     *
     * @param projectId 项目 ID
     * @param scriptVersionId 剧本版本 ID
     * @return 正式剧本快照，不存在时返回 null
     */
    private ScreenplaySnapshot loadLatestScreenplaySnapshot(Long projectId, Long scriptVersionId) {
        return screenplaySnapshotMapper.selectOne(
            new LambdaQueryWrapper<ScreenplaySnapshot>()
                .eq(ScreenplaySnapshot::getProjectId, projectId)
                .eq(ScreenplaySnapshot::getScriptVersionId, scriptVersionId)
                .orderByDesc(ScreenplaySnapshot::getId)
                .last("limit 1")
        );
    }

    /**
     * 解析正式剧本快照标题。
     *
     * @param scriptVersion 剧本版本实体
     * @return 正式剧本标题
     */
    private String resolveSnapshotTitle(ScriptVersion scriptVersion) {
        if (StringUtils.hasText(scriptVersion.getTitle())) {
            return scriptVersion.getTitle().trim();
        }
        return "未命名正式剧本";
    }

    /**
     * 构建角色名到角色 ID 的映射。
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
                characterNameMap.put(character.getName().trim(), character.getId().trim());
            }
        }
        return characterNameMap;
    }

    /**
     * 按行拆分正式剧本文本。
     *
     * @param markdownContent 正式剧本 Markdown 内容
     * @return 行文本列表
     */
    private List<String> splitLines(String markdownContent) {
        return List.of(markdownContent.replace("\r\n", "\n").replace('\r', '\n').split("\n"));
    }

    /**
     * 提取剧集标题。
     *
     * @param line 当前行文本
     * @return 剧集标题，若当前行不是剧集标题则返回 null
     */
    private String extractEpisodeTitle(String line) {
        Matcher matcher = EPISODE_TITLE_PATTERN.matcher(line);
        if (matcher.matches()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * 判断当前行是否像场景标题行。
     *
     * @param line 当前行文本
     * @return 是否可识别为 slugline
     */
    private boolean looksLikeSlugline(String line) {
        return line.startsWith("INT.")
            || line.startsWith("EXT.")
            || line.startsWith("INT/EXT.")
            || line.startsWith("I/E.");
    }

    /**
     * 判断当前行是否属于剧本结构控制行。
     *
     * @param line 当前行文本
     * @return 是否为结构行
     */
    private boolean isStructureLine(String line) {
        return line.startsWith("# ")
            || line.startsWith("## ")
            || line.startsWith("### ")
            || line.startsWith("> 剧集前提：")
            || line.startsWith("目的：")
            || TRANSITION_PATTERN.matcher(line).matches();
    }

    /**
     * 解析后的正式剧本根结构。
     */
    private static class EditableScreenplay {

        /** 解析后的剧集列表。 */
        private final List<EditableEpisode> episodes = new ArrayList<>();

        /**
         * 返回解析后的剧集列表。
         *
         * @return 剧集列表
         */
        public List<EditableEpisode> getEpisodes() {
            return episodes;
        }
    }

    /**
     * 解析后的单集可编辑结构。
     */
    private static class EditableEpisode {

        /** 剧集标题。 */
        private String title;

        /** 剧集下的场景列表。 */
        private final List<EditableScene> scenes = new ArrayList<>();

        /**
         * 返回剧集标题。
         *
         * @return 剧集标题
         */
        public String getTitle() {
            return title;
        }

        /**
         * 设置剧集标题。
         *
         * @param title 剧集标题
         */
        public void setTitle(String title) {
            this.title = title;
        }

        /**
         * 返回剧集下的场景列表。
         *
         * @return 场景列表
         */
        public List<EditableScene> getScenes() {
            return scenes;
        }
    }

    /**
     * 解析后的单场可编辑结构。
     */
    private static class EditableScene {

        /** 场景标题行。 */
        private String slugline;

        /** 动作段列表。 */
        private final List<String> actions = new ArrayList<>();

        /** 对白列表。 */
        private final List<EditableDialogue> dialogues = new ArrayList<>();

        /** 转场提示。 */
        private String transition;

        /**
         * 返回场景标题行。
         *
         * @return 场景标题行
         */
        public String getSlugline() {
            return slugline;
        }

        /**
         * 设置场景标题行。
         *
         * @param slugline 场景标题行
         */
        public void setSlugline(String slugline) {
            this.slugline = slugline;
        }

        /**
         * 返回动作段列表。
         *
         * @return 动作段列表
         */
        public List<String> getActions() {
            return actions;
        }

        /**
         * 返回对白列表。
         *
         * @return 对白列表
         */
        public List<EditableDialogue> getDialogues() {
            return dialogues;
        }

        /**
         * 返回转场提示。
         *
         * @return 转场提示
         */
        public String getTransition() {
            return transition;
        }

        /**
         * 设置转场提示。
         *
         * @param transition 转场提示
         */
        public void setTransition(String transition) {
            this.transition = transition;
        }
    }

    /**
     * 解析后的对白结构。
     */
    private static class EditableDialogue {

        /** 对白角色 ID。 */
        private String characterId;

        /** 括注内容。 */
        private String parenthetical;

        /** 台词内容。 */
        private String line;

        /** 潜台词内容。 */
        private String subtext;

        /**
         * 返回角色 ID。
         *
         * @return 角色 ID
         */
        public String getCharacterId() {
            return characterId;
        }

        /**
         * 设置角色 ID。
         *
         * @param characterId 角色 ID
         */
        public void setCharacterId(String characterId) {
            this.characterId = characterId;
        }

        /**
         * 返回括注内容。
         *
         * @return 括注内容
         */
        public String getParenthetical() {
            return parenthetical;
        }

        /**
         * 设置括注内容。
         *
         * @param parenthetical 括注内容
         */
        public void setParenthetical(String parenthetical) {
            this.parenthetical = parenthetical;
        }

        /**
         * 返回台词内容。
         *
         * @return 台词内容
         */
        public String getLine() {
            return line;
        }

        /**
         * 设置台词内容。
         *
         * @param line 台词内容
         */
        public void setLine(String line) {
            this.line = line;
        }

        /**
         * 返回潜台词内容。
         *
         * @return 潜台词内容
         */
        public String getSubtext() {
            return subtext;
        }

        /**
         * 设置潜台词内容。
         *
         * @param subtext 潜台词内容
         */
        public void setSubtext(String subtext) {
            this.subtext = subtext;
        }
    }
}
