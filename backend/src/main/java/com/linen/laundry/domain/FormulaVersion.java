package com.linen.laundry.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

/**
 * 洗涤配方版本：按布草品类维护预洗→折叠八道工序的目标水温、洗涤剂用量与适用设备类型、最大装载比例。
 *
 * 状态机：DRAFT 草稿（可反复调整）→ PUBLISHED 当前生效；PUBLISHED → ARCHIVED 历史归档。
 * 已发布版本不可原地改写，只能复制（copy_from_id）成新草稿再发布；每品类至多一个 PUBLISHED。
 */
@Data
public class FormulaVersion {

    private Long id;
    private String versionNo;
    private Long categoryId;
    /** 品类内版本序号（V1/V2…） */
    private Integer versionSeq;
    /** DRAFT 草稿 / PUBLISHED 当前生效 / ARCHIVED 历史归档 */
    private String status;
    /** 可承接设备类型，逗号分隔，如 WASHER / WASHER,DRYER,IRONER */
    private String machineTypes;
    /** 最大装载比例（占设备额定容量），如 0.90 */
    private BigDecimal maxLoadRatio;
    /** 水温容差 ±℃ */
    private Integer tempTolerance;
    /** 洗涤剂用量容差 ±% */
    private BigDecimal detergentTolerance;
    /** 由哪个版本复制而来 */
    private Long copyFromId;
    private String createdBy;
    private String publishedBy;
    private LocalDateTime publishedAt;
    private String remark;
    private LocalDateTime createdAt;

    // ------- 非表字段 -------
    private String categoryName;
    private String categoryCode;
    private String statusName;
    private String copyFromVersionNo;
    /** 设备类型中文名列表 */
    private List<String> machineTypeNames;
    /** 是否为该品类当前生效版本 */
    private Boolean current;
    /** 八道工序参数（嵌套 @Many 带出） */
    private List<FormulaStepParam> steps;
}
