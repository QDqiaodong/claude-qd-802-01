package com.linen.laundry.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * 批次参数超差复核记录：推进工序时实际水温 / 洗涤剂超出批次配方快照容差，先转待复核而非直接前进。
 * 主管填写原因后选择 PASS 按本批次放行 或 REWORK 退回重做；结论只作用于本批次，不改写配方。
 */
@Data
public class BatchFormulaReview {

    private Long id;
    private Long batchId;
    private String stepCode;
    private String stepName;
    private Integer stepOrder;
    private Long formulaVersionId;
    private String formulaVersionNo;
    private Integer targetTemp;
    private Integer actualTemp;
    private Integer targetDetergentMl;
    private Integer actualDetergentMl;
    private Integer tempTolerance;
    private BigDecimal detergentTolerance;
    /** 超差项与幅度说明 */
    private String deviationDesc;
    /** PENDING 待复核 / PASS 按本批次放行 / REWORK 退回重做 */
    private String status;
    private String reviewReason;
    private String reviewer;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;

    // ------- 非表字段 -------
    private String batchNo;
    private String statusName;
    private String categoryName;
}
