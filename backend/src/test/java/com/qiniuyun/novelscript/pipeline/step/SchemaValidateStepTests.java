package com.qiniuyun.novelscript.pipeline.step;

import static org.assertj.core.api.Assertions.assertThat;

import com.qiniuyun.novelscript.pipeline.model.SchemaValidationResult;
import org.junit.jupiter.api.Test;

/**
 * 验证 YAML Schema 校验步骤的行为。
 */
class SchemaValidateStepTests {

    @Test
    void test_p3_c4_schema_validate_success() {
        SchemaValidateStep schemaValidateStep = new SchemaValidateStep();

        SchemaValidationResult result = schemaValidateStep.execute("""
            schema_version: "1.0"
            project:
              id: "project_1001"
              title: "长夜余烬"
              source_chapters: [1, 2, 3]
              adaptation_mode: "novel_to_screenplay"
            story_bible:
              characters:
                - id: "char_shenyan"
                  name: "沈砚"
              relationships: []
              locations: []
              timeline: []
              conflicts: []
              foreshadowing: []
              adaptation_strategy: []
            episodes:
              - id: "ep01"
                title: "旧城疑影"
                premise: "引出主线"
                source_refs: ["chapter:1"]
                scenes:
                  - id: "sc01"
                    slugline: "夜 外 旧城巷口"
                    purpose: "建立悬疑氛围"
                    source_refs: ["chapter:1"]
                    characters: ["char_shenyan"]
                    actions: ["沈砚停下脚步。"]
                    beats: []
                    dialogue:
                      - character_id: "char_shenyan"
                        line: "这里昨晚一定出过事。"
                    transition: "CUT_TO"
            metadata:
              generated_at: "2026-06-06T14:10:00+08:00"
              generator: "deepseek-chat"
              notes: []
            """);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void test_p3_c4_schema_validate_failure() {
        SchemaValidateStep schemaValidateStep = new SchemaValidateStep();

        SchemaValidationResult result = schemaValidateStep.execute("""
            schema_version: "1.0"
            project:
              id: "project_1001"
            story_bible:
              characters: []
              relationships: []
              locations: []
              timeline: []
              conflicts: []
              foreshadowing: []
              adaptation_strategy: []
            episodes:
              - id: "ep01"
                scenes:
                  - id: "sc01"
                    source_refs: ["chapter:9"]
            metadata:
              generator: "deepseek-chat"
            """);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).extracting("path")
            .contains("project.title", "episodes[0].scenes[0].slugline");
    }
}
