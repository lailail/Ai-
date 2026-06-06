package com.qiniuyun.novelscript.pipeline.step;

import static org.assertj.core.api.Assertions.assertThat;

import com.qiniuyun.novelscript.pipeline.model.ScriptDocument;
import com.qiniuyun.novelscript.pipeline.model.ScriptDocumentMetadata;
import com.qiniuyun.novelscript.pipeline.model.ScriptDocumentProject;
import com.qiniuyun.novelscript.pipeline.model.ScriptEpisodeResult;
import com.qiniuyun.novelscript.pipeline.model.ScriptSceneDialogue;
import com.qiniuyun.novelscript.pipeline.model.ScriptSceneResult;
import com.qiniuyun.novelscript.pipeline.model.StoryBibleCharacter;
import com.qiniuyun.novelscript.pipeline.model.StoryBibleResult;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 验证 YAML 序列化步骤的输出行为。
 */
class YamlSerializeStepTests {

    /**
     * 验证结构化剧本文档可以被序列化为 YAML。
     */
    @Test
    void test_p3_c4_yaml_serialize() {
        YamlSerializeStep yamlSerializeStep = new YamlSerializeStep();

        String yaml = yamlSerializeStep.execute(buildScriptDocument());

        assertThat(yaml).contains("schema_version: \"1.0\"");
        assertThat(yaml).contains("title: \"Long Night Ember\"");
        assertThat(yaml).contains("episodes:");
        assertThat(yaml).contains("slugline: \"EXT. OLD TOWN ALLEY - NIGHT\"");
        assertThat(yaml).contains("character_id: \"char_shenyan\"");
    }

    /**
     * 构造测试用剧本文档。
     *
     * @return 剧本文档
     */
    private ScriptDocument buildScriptDocument() {
        ScriptDocument document = new ScriptDocument();
        document.setSchemaVersion("1.0");

        ScriptDocumentProject project = new ScriptDocumentProject();
        project.setId("project_1001");
        project.setTitle("Long Night Ember");
        project.setSourceChapters(List.of(1, 2, 3));
        project.setAdaptationMode("novel_to_screenplay");
        document.setProject(project);

        StoryBibleResult storyBible = new StoryBibleResult();
        StoryBibleCharacter character = new StoryBibleCharacter();
        character.setId("char_shenyan");
        character.setName("Shen Yan");
        storyBible.setCharacters(List.of(character));
        document.setStoryBible(storyBible);

        ScriptSceneResult scene = new ScriptSceneResult();
        scene.setId("sc01");
        scene.setSlugline("EXT. OLD TOWN ALLEY - NIGHT");
        scene.setPurpose("Set the suspense tone");
        scene.setSourceRefs(List.of("chapter:1"));
        scene.setCharacters(List.of("char_shenyan"));
        scene.setActions(List.of("Shen Yan slows to a stop."));
        ScriptSceneDialogue dialogue = new ScriptSceneDialogue();
        dialogue.setCharacterId("char_shenyan");
        dialogue.setLine("Something happened here.");
        scene.setDialogue(List.of(dialogue));
        document.setEpisodes(List.of(
            ScriptEpisodeResult.fromOutline("ep01", "Shadow Over The Old Town", "Introduce the main suspense line", List.of("chapter:1"), List.of(scene))
        ));

        ScriptDocumentMetadata metadata = new ScriptDocumentMetadata();
        metadata.setGeneratedAt(OffsetDateTime.parse("2026-06-06T14:10:00+08:00"));
        metadata.setGenerator("deepseek-chat");
        metadata.setNotes(List.of("demo"));
        document.setMetadata(metadata);
        return document;
    }
}
