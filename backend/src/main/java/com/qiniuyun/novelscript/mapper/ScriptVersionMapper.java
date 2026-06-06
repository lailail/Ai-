package com.qiniuyun.novelscript.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiniuyun.novelscript.domain.entity.ScriptVersion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 剧本版本的数据访问接口。
 */
@Mapper
public interface ScriptVersionMapper extends BaseMapper<ScriptVersion> {
}
