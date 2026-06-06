package com.qiniuyun.novelscript.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiniuyun.novelscript.domain.entity.YamlSnapshot;
import org.apache.ibatis.annotations.Mapper;

/**
 * YAML 快照的数据访问接口。
 */
@Mapper
public interface YamlSnapshotMapper extends BaseMapper<YamlSnapshot> {
}
