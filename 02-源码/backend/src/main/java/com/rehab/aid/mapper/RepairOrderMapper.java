package com.rehab.aid.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rehab.aid.entity.RepairOrder;
import org.apache.ibatis.annotations.Mapper;

/** 故障维修工单 Mapper */
@Mapper
public interface RepairOrderMapper extends BaseMapper<RepairOrder> {
}
