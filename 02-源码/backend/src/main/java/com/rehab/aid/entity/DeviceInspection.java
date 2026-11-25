package com.rehab.aid.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 检测消毒记录 */
@Data
@TableName("device_inspection")
public class DeviceInspection {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 检测单号 */
    private String inspectionNo;

    /** 器具ID */
    private Long deviceId;

    /** 器具编号 */
    private String deviceNo;

    /** 器具类别 */
    private String category;

    /** 检查项 */
    private String checkItems;

    /** 消毒方式 */
    private String disinfectMethod;

    /** 结论：PASS/FAIL/PENDING */
    private String result;

    /** 执行人 */
    private String inspector;

    /** 检测时间 */
    private LocalDateTime inspectTime;

    /** 下次到期 */
    private LocalDate nextDueDate;

    /** 现场照片 */
    private String photoUrl;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;
}
