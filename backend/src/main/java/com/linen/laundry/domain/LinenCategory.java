package com.linen.laundry.domain;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

/**
 * 布草品类台账：床单 / 被套 / 枕套 / 浴巾 / 台布 / 工服。
 */
@Data
public class LinenCategory {

    private Long id;
    private String code;
    private String name;
    /** 规格，如 1.5m×2.0m */
    private String spec;
    /** 洗涤单价（元/件） */
    private BigDecimal unitPrice;
    /** 在库数（净布草） */
    private Integer stockQty;
    /** 报废率 % */
    private BigDecimal scrapRate;
    /** 最低周转量 */
    private Integer minTurnover;
    /** 归属：HOTEL / HOSPITAL / COMMON */
    private String ownerType;
    /** 当前生效洗涤配方版本 id；为空表示无当前版本，新批次不可排产 */
    private Long currentFormulaVersionId;
    /** 当前版本切换序号：发布 / 停用切换走 CAS 乐观锁 */
    private Integer formulaSeq;
    private String remark;

    // ------- 非表字段 -------
    /** 周转缺口 = max(0, 最低周转量 - 在库数) */
    private Integer gap;
    /** 在库金额 */
    private BigDecimal stockAmount;
    /** 当前生效配方版本号（查询拼装） */
    private String currentFormulaVersionNo;
    /** 近期洗涤批次（嵌套 @Many 带出） */
    private List<WashBatch> recentBatches;
}
