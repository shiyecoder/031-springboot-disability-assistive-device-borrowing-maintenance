package com.rehab.aid.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 菜单 */
@Data
@TableName("sys_menu")
public class SysMenu {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 父级ID */
    private Long parentId;

    /** 菜单名称 */
    private String name;

    /** 路由 */
    private String path;

    /** 图标 */
    private String icon;

    /** 排序 */
    private Integer sort;

    /** 可见角色 */
    private String roleCodes;
}
