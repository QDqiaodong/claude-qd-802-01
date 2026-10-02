package com.linen.laundry.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

/**
 * 洗涤批次：一缸布草从预洗到折叠的流转主体。
 */
@Data
public class WashBatch {

    private Long id;
    private String batchNo;
    private Long categoryId;
    /** 重量 kg */
    private BigDecimal weightKg;
    /** 当前工序编码 */
    private String currentStep;
    /** 当前工序号 1~8 */
    private Integer currentStepOrder;
    /** 洗涤剂用量 ml */
    private Integer detergentMl;
    /** 水温 ℃ */
    private Integer waterTemp;
    private Long machineId;
    private String operator;
    /** RUNNING 在产 / DONE 已完成 / HOLD 挂起 / QUARANTINED 污染隔离中 */
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    /** 若本批次是污染复洗产生的新批次，对应复洗单 id */
    private Long rewashOrderId;
    /** 被污染命中隔离前是否已是 DONE（放行时据此决定恢复 RUNNING 还是保持 DONE） */
    private Boolean quarantineWasDone;
    /** 排产时所用洗涤配方版本 id（仅留存，不实时回读配方表） */
    private Long formulaVersionId;
    /** 排产时配方版本号（冗余） */
    private String formulaVersionNo;
    /** 排产时完整配方快照原始 JSON */
    private String formulaSnapshot;
    /** NONE 正常 / PENDING 参数超差待主管复核（status 同步为 REVIEW） */
    private String reviewStatus;
    private String remark;

    // ------- 非表字段 -------
    private String categoryName;
    private String machineName;
    private String machineCode;
    /** 进度百分比 */
    private Integer progress;
    /** 解析后的配方快照（隧道 / 详情展示标准参数、超差判定都用它） */
    private com.linen.laundry.dto.BatchFormulaSnapshot formula;
    /** 当前待复核记录（review_status=PENDING 时带出） */
    private com.linen.laundry.domain.BatchFormulaReview pendingReview;
    /** 工序流转记录（嵌套 @Many 带出） */
    private List<BatchStep> steps;
    /** 品类（嵌套 @One 带出） */
    private LinenCategory category;
    /** 设备（嵌套 @One 带出） */
    private Machine machine;

    // ------- 污染隔离 / 复洗追溯（非表字段，查询拼装） -------
    /** 当前是否处于污染隔离中（存在 ISOLATED 记录） */
    private Boolean quarantined;
    /** 命中的污染事件 id（仍隔离中时） */
    private Long quarantineEventId;
    /** 命中的污染事件号 */
    private String quarantineEventNo;
    /** 污染类型中文名 */
    private String quarantineTypeName;
    /** 隔离记录 id */
    private Long isolationId;
    /** 命中原因 */
    private String quarantineReason;
    /** 作为复洗批次时：复洗单状态 */
    private String rewashStatus;
    /** 作为复洗批次时：原批次号 */
    private String sourceBatchNo;
    /** 作为原隔离批次时：有效复洗批次号 */
    private String rewashBatchNo;
}
