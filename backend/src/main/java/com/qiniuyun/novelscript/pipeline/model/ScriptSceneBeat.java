package com.qiniuyun.novelscript.pipeline.model;

import lombok.Data;

/**
 * 场景中的动作节拍。
 */
@Data
public class ScriptSceneBeat {

    /**
     * 节拍 ID。
     */
    private String id;

    /**
     * 节拍动作说明。
     */
    private String action;
}
