package com.rehab.aid.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 借用维护单 */
@Data
@TableName("borrow_application")
public class BorrowApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 业务编号 */
    private String appNo;

    /** 使用人姓名 */
    private String applicantName;

    /** 联系电话 */
    private String applicantPhone;

    /** 代办人 */
    private String agentName;

    /** 身体条件 */
    private String userCondition;

    /** 申请器具类别 */
    private String deviceCategory;

    /** 期望开始 */
    private LocalDate expectStart;

    /** 期望结束 */
    private LocalDate expectEnd;

    /** 用途说明 */
    private String purpose;

    /** 配送方式：SELF/DELIVER */
    private String deliveryType;

    /** 需求证明 */
    private String proofUrl;

    /** 状态 */
    private String status;

    /** 当前阶段 */
    private String currentStage;

    /** 责任人 */
    private String ownerName;

    /** 评估员 */
    private String assessorName;

    /** 锁定器具ID */
    private Long deviceId;

    /** 锁定器具编号 */
    private String deviceNo;

    /** 借用天数 */
    private Integer borrowDays;

    /** 是否逾期 */
    private Integer overdue;

    /** 退回/驳回原因 */
    private String rejectReason;

    /** 创建人ID */
    private Long createBy;

    /** 创建人 */
    private String createByName;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 版本号 */
    private Integer version;
}
