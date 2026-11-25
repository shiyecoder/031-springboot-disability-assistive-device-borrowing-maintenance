package com.rehab.aid.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 系统用户 */
@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 账号 */
    private String username;

    /** 密码（BCrypt） */
    private String password;

    /** 姓名 */
    private String realName;

    /** 手机号 */
    private String phone;

    /** 头像 */
    private String avatar;

    /** 角色编码 */
    private String roleCode;

    /** 所属机构 */
    private String orgName;

    /** 状态 */
    private Integer status;

    /** 最后登录 */
    private LocalDateTime lastLogin;

    /** 创建时间 */
    private LocalDateTime createTime;
}
