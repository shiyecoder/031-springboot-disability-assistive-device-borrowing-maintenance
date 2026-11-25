package com.rehab.aid.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 残障辅助器具台账 */
@Data
@TableName("assistive_device")
public class AssistiveDevice {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 器具编号 */
    private String deviceNo;

    /** 器具名称 */
    private String name;

    /** 器具类别 */
    private String category;

    /** 适用人群 */
    private String suitPeople;

    /** 品牌 */
    private String brand;

    /** 规格型号 */
    private String spec;

    /** 载重 */
    private String loadCapacity;

    /** 适用说明 */
    private String capacityDesc;

    /** 来源 */
    private String source;

    /** 入库日期 */
    private LocalDate inboundDate;

    /** 存放点 */
    private String location;

    /** 状态：AVAILABLE/RESERVED/LENT/TO_DISINFECT/TO_REPAIR/SCRAPPED */
    private String status;

    /** 当前状况 */
    private String currentCondition;

    /** 器具图片 */
    private String imageUrl;

    /** 累计维护次数 */
    private Integer maintainCount;

    /** 最近维护日期 */
    private LocalDate lastMaintainDate;

    /** 备注 */
    private String remark;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除 */
    private Integer deleted;

    /** 版本号 */
    private Integer version;
}
