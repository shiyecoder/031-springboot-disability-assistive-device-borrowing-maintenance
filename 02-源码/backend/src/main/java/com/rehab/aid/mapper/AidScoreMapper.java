package com.rehab.aid.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rehab.aid.entity.AidScore;
import org.apache.ibatis.annotations.Mapper;

/** 适配度计算结果留痕 Mapper */
@Mapper
public interface AidScoreMapper extends BaseMapper<AidScore> {
}
