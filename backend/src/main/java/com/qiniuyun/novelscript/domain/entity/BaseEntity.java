package com.qiniuyun.novelscript.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 基础实体，统一保存主键和时间字段。
 */
@Getter
@Setter
public class BaseEntity {

    /** 主键 ID。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 记录创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 记录最后更新时间。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

}
