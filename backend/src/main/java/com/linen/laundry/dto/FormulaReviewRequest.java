package com.linen.laundry.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 主管对参数超差批次的复核结论：PASS 按本批次放行 / REWORK 退回重做。
 * 结论只作用于本批次，不改写配方与其它批次。
 */
@Data
public class FormulaReviewRequest {

    /** PASS 按本批次放行 / REWORK 退回重做 */
    @NotBlank(message = "请选择复核结论：放行或退回重做")
    private String decision;

    @NotBlank(message = "请主管填写复核原因")
    private String reason;

    private String reviewer;
}
