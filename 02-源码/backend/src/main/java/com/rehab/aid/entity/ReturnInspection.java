package com.rehab.aid.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 归还检查 */
@Data
@TableName("return_inspection")
public class ReturnInspection {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 归还单号 */
    private String returnNo;

    /** 业务ID */
    private Long appId;

    /** 业务编号 */
    private String appNo;

    /** 器具ID */
    private Long deviceId;

    /** 器具编号 */
    private String deviceNo;

    /** 实际归还时间 */
    private LocalDateTime actualReturnTime;

    /** 功能检查 */
    private String functionCheck;

    /** 外观检查 */
    private String appearanceCheck;

    /** 配件检查 */
    private String partsCheck;

    /** 清洁状态 */
    private String cleanStatus;

    /** 损坏程度 */
    private String damageLevel;

    /** 损坏责任 */
    private String damageDuty;

    /** 处理结论：TO_DISINFECT/TO_REPAIR/SCRAP */
    private String conclusion;

    /** 归还照片 */
    private String photoUrl;

    /** 检查人 */
    private String inspector;

    /** 检查时间 */
    private LocalDateTime inspectTime;

    /** 备注 */
    private String remark;
}
