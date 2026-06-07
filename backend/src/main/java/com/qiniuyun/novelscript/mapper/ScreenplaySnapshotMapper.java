package com.qiniuyun.novelscript.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiniuyun.novelscript.domain.entity.ScreenplaySnapshot;
import org.apache.ibatis.annotations.Mapper;

/**
 * 正式剧本快照的数据访问接口。
 */
@Mapper
public interface ScreenplaySnapshotMapper extends BaseMapper<ScreenplaySnapshot> {
}
