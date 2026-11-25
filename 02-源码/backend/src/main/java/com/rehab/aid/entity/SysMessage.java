package com.rehab.aid.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 站内消息 */
@Data
@TableName("sys_message")
public class SysMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 业务编号 */
    private String bizNo;

    /** 跳转地址 */
    private String link;

    /** 接收人ID */
    private Long receiverId;

    /** 接收角色 */
    private String receiverRole;

    /** 是否已读 */
    private Integer isRead;

    /** 创建时间 */
    private LocalDateTime createTime;
}
