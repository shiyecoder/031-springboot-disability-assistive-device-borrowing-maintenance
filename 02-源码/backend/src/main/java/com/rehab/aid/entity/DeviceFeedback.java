package com.rehab.aid.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 回访评价 */
@Data
@TableName("device_feedback")
public class DeviceFeedback {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 评价编号 */
    private String feedbackNo;

    /** 业务ID */
    private Long appId;

    /** 业务编号 */
    private String appNo;

    /** 使用人 */
    private String applicantName;

    /** 器具类别 */
    private String deviceCategory;

    /** 适用性评分 */
    private Integer fitScore;

    /** 便利性评分 */
    private Integer easyScore;

    /** 服务评分 */
    private Integer serviceScore;

    /** 建议与问题 */
    private String content;

    /** 评价时间 */
    private LocalDateTime feedbackTime;
}
