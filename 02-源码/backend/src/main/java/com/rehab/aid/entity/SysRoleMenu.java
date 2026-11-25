package com.rehab.aid.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 角色菜单 */
@Data
@TableName("sys_role_menu")
public class SysRoleMenu {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 角色ID */
    private Long roleId;

    /** 菜单ID */
    private Long menuId;
}
