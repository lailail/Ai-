package com.qiniuyun.novelscript.pipeline.model;

import lombok.Data;

/**
 * 场景备注信息。
 */
@Data
public class ScriptSceneNotes {

    /**
     * 情绪提示。
     */
    private String emotion;

    /**
     * 节奏提示。
     */
    private String pacing;

    /**
     * 待补充事项。
     */
    private String todo;
}
