package com.rehab.aid.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 字典 */
@Data
@TableName("sys_dict")
public class SysDict {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 字典类型 */
    private String dictType;

    /** 编码 */
    private String dictCode;

    /** 名称 */
    private String dictLabel;

    /** 排序 */
    private Integer sort;
}
