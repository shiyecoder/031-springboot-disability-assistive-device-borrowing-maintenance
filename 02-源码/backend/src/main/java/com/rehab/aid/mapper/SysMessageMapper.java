package com.rehab.aid.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rehab.aid.entity.SysMessage;
import org.apache.ibatis.annotations.Mapper;

/** 站内消息 Mapper */
@Mapper
public interface SysMessageMapper extends BaseMapper<SysMessage> {
}
