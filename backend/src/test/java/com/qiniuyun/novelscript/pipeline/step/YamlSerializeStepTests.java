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

    @Test
    void test_p3_c4_yaml_serialize() {
        YamlSerializeStep yamlSerializeStep = new YamlSerializeStep();

        String yaml = yamlSerializeStep.execute(buildScriptDocument());

        assertThat(yaml).contains("schema_version: \"1.0\"");
        assertThat(yaml).contains("title: \"长夜余烬\"");
        assertThat(yaml).contains("episodes:");
        assertThat(yaml).contains("slugline: \"夜 外 旧城巷口\"");
        assertThat(yaml).contains("character_id: \"char_shenyan\"");
    }

    private ScriptDocument buildScriptDocument() {
        ScriptDocument document = new ScriptDocument();
        document.setSchemaVersion("1.0");

        ScriptDocumentProject project = new ScriptDocumentProject();
        project.setId("project_1001");
        project.setTitle("长夜余烬");
        project.setSourceChapters(List.of(1, 2, 3));
        project.setAdaptationMode("novel_to_screenplay");
        document.setProject(project);

        StoryBibleResult storyBible = new StoryBibleResult();
        StoryBibleCharacter character = new StoryBibleCharacter();
        character.setId("char_shenyan");
        character.setName("沈砚");
        storyBible.setCharacters(List.of(character));
        document.setStoryBible(storyBible);

        ScriptSceneResult scene = new ScriptSceneResult();
        scene.setId("sc01");
        scene.setSlugline("夜 外 旧城巷口");
        scene.setPurpose("建立悬疑氛围");
        scene.setSourceRefs(List.of("chapter:1"));
        scene.setCharacters(List.of("char_shenyan"));
        scene.setActions(List.of("沈砚停下脚步。"));
        ScriptSceneDialogue dialogue = new ScriptSceneDialogue();
        dialogue.setCharacterId("char_shenyan");
        dialogue.setLine("这里昨晚一定出过事。");
        scene.setDialogue(List.of(dialogue));
        document.setEpisodes(List.of(ScriptEpisodeResult.fromOutline("ep01", "旧城疑影", "引出主线", List.of("chapter:1"), List.of(scene))));

        ScriptDocumentMetadata metadata = new ScriptDocumentMetadata();
        metadata.setGeneratedAt(OffsetDateTime.parse("2026-06-06T14:10:00+08:00"));
        metadata.setGenerator("deepseek-chat");
        metadata.setNotes(List.of("demo"));
        document.setMetadata(metadata);
        return document;
    }
}
