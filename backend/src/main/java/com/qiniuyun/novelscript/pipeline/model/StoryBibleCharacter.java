package com.qiniuyun.novelscript.pipeline.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Story Bible 中的角色设定。
 */
@Data
public class StoryBibleCharacter {

    /**
     * 角色唯一标识。
     */
    private String id;

    /**
     * 角色名称。
     */
    private String name;

    /**
     * 角色别名列表。
     */
    private List<String> aliases = new ArrayList<>();

    /**
     * 角色定位，例如主角或配角。
     */
    private String role;

    /**
     * 角色性格特征。
     */
    private List<String> traits = new ArrayList<>();

    /**
     * 角色核心目标。
     */
    private String goal;
}
