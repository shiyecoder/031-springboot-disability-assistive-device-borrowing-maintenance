package com.rehab.aid.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 适配度规则配置 */
@Data
@TableName("aid_rule")
public class AidRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 版本号 */
    private String version;

    /** 各因素权重JSON */
    private String weights;

    /** 等级阈值JSON */
    private String thresholds;

    /** 是否启用 */
    private Integer status;

    /** 生效时间 */
    private LocalDateTime effectTime;

    /** 修改人 */
    private String creator;

    /** 说明 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;
}
