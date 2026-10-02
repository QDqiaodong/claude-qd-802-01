package com.linen.laundry.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 停用当前配方版本：必须先选定接替版本，否则拒绝（无当前版本的品类无法新批次排产）。
 */
@Data
public class FormulaDeactivateRequest {

    /** 接替的、已发布或已归档的版本 id（不能是草稿） */
    @NotNull(message = "停用当前版本前必须先选定接替版本")
    private Long successorId;

    private String operator;
}
