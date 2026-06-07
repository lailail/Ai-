package com.qiniuyun.novelscript.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.qiniuyun.novelscript.pipeline.model.ScriptDocument;
import com.qiniuyun.novelscript.pipeline.model.ScriptDocumentMetadata;
import com.qiniuyun.novelscript.pipeline.model.ScriptDocumentProject;
import com.qiniuyun.novelscript.pipeline.model.ScriptEpisodeResult;
import com.qiniuyun.novelscript.pipeline.model.ScriptSceneDialogue;
import com.qiniuyun.novelscript.pipeline.model.ScriptSceneResult;
import com.qiniuyun.novelscript.pipeline.model.StoryBibleCharacter;
import com.qiniuyun.novelscript.pipeline.model.StoryBibleResult;
import com.qiniuyun.novelscript.service.impl.ScreenplayRenderServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 验证正式剧本规则渲染服务的测试。
 */
class ScreenplayRenderServiceTests {

    /**
     * 验证渲染服务会把 YAML 结构转换为可阅读的正式剧本文本。
     */
    @Test
    void test_p4_screenplay_render_markdown() {
        ScreenplayRenderService screenplayRenderService = new ScreenplayRenderServiceImpl();

        ScriptDocument scriptDocument = new ScriptDocument();
        ScriptDocumentProject project = new ScriptDocumentProject();
        project.setTitle("旧城回声");
        scriptDocument.setProject(project);

        StoryBibleCharacter character = new StoryBibleCharacter();
        character.setId("char_ashu");
        character.setName("阿述");

        StoryBibleResult storyBible = new StoryBibleResult();
        storyBible.setCharacters(List.of(character));
        scriptDocument.setStoryBible(storyBible);

        ScriptSceneDialogue dialogue = new ScriptSceneDialogue();
        dialogue.setCharacterId("char_ashu");
        dialogue.setLine("这里不是我离开的样子。");

        ScriptSceneResult scene = new ScriptSceneResult();
        scene.setId("sc01");
        scene.setSlugline("EXT. 旧城河岸 - NIGHT");
        scene.setPurpose("建立人物回归旧地后的陌生感");
        scene.setActions(List.of(
            "阿述停在河岸边，望着对岸熄灭的灯牌。",
            "风把旧报纸卷过她的脚边。"
        ));
        scene.setDialogue(List.of(dialogue));
        scene.setTransition("CUT_TO");

        ScriptEpisodeResult episode = new ScriptEpisodeResult();
        episode.setId("ep01");
        episode.setTitle("河岸归来");
        episode.setPremise("人物重新踏回故事起点");
        episode.setScenes(List.of(scene));
        scriptDocument.setEpisodes(List.of(episode));

        ScriptDocumentMetadata metadata = new ScriptDocumentMetadata();
        metadata.setGenerator("deepseek-chat");
        scriptDocument.setMetadata(metadata);

        String markdownContent = screenplayRenderService.renderMarkdown(scriptDocument);

        assertThat(markdownContent).contains("河岸归来");
        assertThat(markdownContent).contains("EXT. 旧城河岸 - NIGHT");
        assertThat(markdownContent).contains("阿述");
        assertThat(markdownContent).contains("这里不是我离开的样子。");
        assertThat(markdownContent).contains("CUT_TO");
    }
}
