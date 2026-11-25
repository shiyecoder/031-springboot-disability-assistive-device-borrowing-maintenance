package com.rehab.aid.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 库存预留 */
@Data
@TableName("device_reservation")
public class DeviceReservation {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 预留单号 */
    private String reserveNo;

    /** 业务ID */
    private Long appId;

    /** 业务编号 */
    private String appNo;

    /** 器具ID */
    private Long deviceId;

    /** 器具编号 */
    private String deviceNo;

    /** 器具名称 */
    private String deviceName;

    /** 锁定时间 */
    private LocalDateTime lockTime;

    /** 到期时间 */
    private LocalDateTime expireTime;

    /** 状态：LOCKED/RELEASED/HANDOVER */
    private String status;

    /** 释放原因 */
    private String releaseReason;

    /** 创建时间 */
    private LocalDateTime createTime;
}
