package com.linen.laundry.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

/**
 * 批次完整配方快照：排产时从当前 PUBLISHED 配方深拷贝落库（wash_batch.formula_snapshot）。
 * 批次隧道与详情、超差复核只认快照——后续发布新版本或停用切换都不会改写在产批次。
 */
@Data
public class BatchFormulaSnapshot {

    private Long versionId;
    private String versionNo;
    private Long categoryId;
    /** 可承接设备类型（已拆分为列表） */
    private List<String> machineTypes;
    private BigDecimal maxLoadRatio;
    private Integer tempTolerance;
    private BigDecimal detergentTolerance;
    private List<Step> steps;

    /** 单道工序的标准参数 */
    @Data
    public static class Step {
        private String stepCode;
        private String stepName;
        private Integer stepOrder;
        private Integer targetTemp;
        private Integer targetDetergentMl;
    }

    public Step stepByCode(String stepCode) {
        if (steps == null) {
            return null;
        }
        return steps.stream().filter(s -> s.getStepCode().equals(stepCode)).findFirst().orElse(null);
    }
}
