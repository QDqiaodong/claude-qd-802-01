package com.linen.laundry.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 新建 / 调整配方草稿。仅 DRAFT 可被此请求反复覆盖；发布后版本不可再用此接口改写。
 */
@Data
public class FormulaVersionRequest {

    @NotNull(message = "请选择布草品类")
    private Long categoryId;

    /** 可承接设备类型（至少一种） */
    @NotEmpty(message = "至少选择一种可承接设备类型")
    private List<String> machineTypes;

    @NotNull(message = "请填写最大装载比例")
    private BigDecimal maxLoadRatio;

    /** 水温容差 ±℃，留空默认 5 */
    private Integer tempTolerance;

    /** 洗涤剂用量容差 ±%，留空默认 10 */
    private BigDecimal detergentTolerance;

    private String createdBy;
    private String remark;

    /** 八道工序参数，发布前必须恰好覆盖预洗…折叠八道 */
    @NotEmpty(message = "请填写八道工序参数")
    private List<StepParam> steps;

    @Data
    public static class StepParam {
        @NotNull(message = "工序编码不能为空")
        private String stepCode;
        @NotNull(message = "目标水温不能为空")
        private Integer targetTemp;
        @NotNull(message = "目标洗涤剂用量不能为空")
        private Integer targetDetergentMl;
    }
}
