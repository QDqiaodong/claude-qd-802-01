package com.linen.laundry.domain;

import lombok.Data;

/**
 * 配方工序参数：一道工序的目标水温与目标洗涤剂用量。
 */
@Data
public class FormulaStepParam {

    private Long id;
    private Long versionId;
    private String stepCode;
    private String stepName;
    /** 工序号 1~8 */
    private Integer stepOrder;
    /** 目标水温 ℃（脱水 / 折叠可为 0） */
    private Integer targetTemp;
    /** 目标洗涤剂用量 ml（过水 / 脱水 / 烫平 / 折叠可为 0） */
    private Integer targetDetergentMl;
}
