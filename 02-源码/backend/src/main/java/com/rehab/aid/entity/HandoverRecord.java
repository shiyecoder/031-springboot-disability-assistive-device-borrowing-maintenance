package com.rehab.aid.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 交接记录 */
@Data
@TableName("handover_record")
public class HandoverRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 交接单号 */
    private String handoverNo;

    /** 业务ID */
    private Long appId;

    /** 业务编号 */
    private String appNo;

    /** 器具ID */
    private Long deviceId;

    /** 器具编号 */
    private String deviceNo;

    /** 接收人 */
    private String receiverName;

    /** 状况检查 */
    private String conditionCheck;

    /** 配件清单 */
    private String parts;

    /** 押金线下状态 */
    private String depositStatus;

    /** 押金金额 */
    private BigDecimal depositAmount;

    /** 使用指导确认 */
    private Integer guideConfirm;

    /** 交接照片 */
    private String photoUrl;

    /** 交接时间 */
    private LocalDateTime handoverTime;

    /** 经办人 */
    private String operatorName;

    /** 备注 */
    private String remark;
}
