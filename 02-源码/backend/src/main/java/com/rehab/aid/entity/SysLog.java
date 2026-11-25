package com.rehab.aid.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 操作日志 */
@Data
@TableName("sys_log")
public class SysLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 操作人 */
    private String operator;

    /** 操作 */
    private String action;

    /** 业务编号 */
    private String bizNo;

    /** 详情 */
    private String detail;

    /** IP */
    private String ip;

    /** 创建时间 */
    private LocalDateTime createTime;
}
