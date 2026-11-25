package com.rehab.aid.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 故障维修工单 */
@Data
@TableName("repair_order")
public class RepairOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 工单号 */
    private String orderNo;

    /** 器具ID */
    private Long deviceId;

    /** 器具编号 */
    private String deviceNo;

    /** 器具名称 */
    private String deviceName;

    /** 来源业务ID */
    private Long appId;

    /** 来源业务编号 */
    private String appNo;

    /** 故障类型 */
    private String faultType;

    /** 故障描述 */
    private String faultDesc;

    /** 维修措施 */
    private String repairMeasure;

    /** 更换零件 */
    private String partsReplaced;

    /** 维修费用 */
    private BigDecimal cost;

    /** 报修时间 */
    private LocalDateTime reportTime;

    /** 完工时间 */
    private LocalDateTime finishTime;

    /** 完工检测结果 */
    private String checkResult;

    /** 是否建议报废 */
    private Integer scrapSuggest;

    /** 状态：PENDING/REPAIRING/CHECKING/DONE/SCRAPPED */
    private String status;

    /** 维修员 */
    private String repairerName;

    /** 维修照片 */
    private String photoUrl;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;
}
