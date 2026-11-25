package com.rehab.aid.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 角色 */
@Data
@TableName("sys_role")
public class SysRole {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 角色编码 */
    private String roleCode;

    /** 角色名称 */
    private String roleName;

    /** 数据范围 */
    private String dataScope;

    /** 说明 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;
}
