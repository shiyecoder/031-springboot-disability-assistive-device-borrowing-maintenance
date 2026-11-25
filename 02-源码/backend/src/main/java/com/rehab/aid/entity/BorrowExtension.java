package com.rehab.aid.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 延期申请 */
@Data
@TableName("borrow_extension")
public class BorrowExtension {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 延期单号 */
    private String extNo;

    /** 业务ID */
    private Long appId;

    /** 业务编号 */
    private String appNo;

    /** 原到期日 */
    private LocalDate oldEnd;

    /** 申请新到期日 */
    private LocalDate newEnd;

    /** 延期原因 */
    private String reason;

    /** 状态：PENDING/APPROVED/REJECTED */
    private String status;

    /** 申请时间 */
    private LocalDateTime applyTime;

    /** 审批时间 */
    private LocalDateTime approveTime;

    /** 审批人 */
    private String approver;

    /** 审批意见 */
    private String approveRemark;
}
