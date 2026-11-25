package com.rehab.aid.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 适配评估 */
@Data
@TableName("adaptation_assessment")
public class AdaptationAssessment {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 业务ID */
    private Long appId;

    /** 业务编号 */
    private String appNo;

    /** 评估员 */
    private String assessorName;

    /** 建议型号 */
    private String suggestModel;

    /** 建议附件 */
    private String suggestParts;

    /** 借用时长 */
    private Integer borrowDays;

    /** 是否需保证金 */
    private Integer needDeposit;

    /** 保证金金额 */
    private BigDecimal depositAmount;

    /** 使用指导 */
    private String guidance;

    /** 评估结论 */
    private String conclusion;

    /** 评估时间 */
    private LocalDateTime assessTime;

    /** 备注 */
    private String remark;
}
