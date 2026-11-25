package com.rehab.aid.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 适配度计算结果留痕 */
@Data
@TableName("aid_score")
public class AidScore {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 计算编号 */
    private String scoreNo;

    /** 业务ID */
    private Long appId;

    /** 业务编号 */
    private String appNo;

    /** 规则版本 */
    private String ruleVersion;

    /** 输入快照JSON */
    private String inputSnapshot;

    /** 各项得分JSON */
    private String detailJson;

    /** 总分 */
    private BigDecimal totalScore;

    /** 等级：A/B/C/D */
    private String level;

    /** 维护等级 */
    private String maintainLevel;

    /** 推荐结论 */
    private String suggestText;

    /** 未满足条件与可改进项 */
    private String adviceJson;

    /** 人工调整后总分 */
    private BigDecimal manualScore;

    /** 人工调整后等级 */
    private String manualLevel;

    /** 人工调整原因 */
    private String manualReason;

    /** 人工调整人 */
    private String manually;

    /** 人工调整时间 */
    private LocalDateTime manualTime;

    /** 计算时间 */
    private LocalDateTime calcTime;
}
