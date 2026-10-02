package com.linen.laundry.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linen.laundry.domain.FormulaStepParam;
import com.linen.laundry.domain.FormulaVersion;
import com.linen.laundry.dto.BatchFormulaSnapshot;

/**
 * 配方快照与超差判定的纯函数工具：
 * 批次建批时把当前 PUBLISHED 配方深拷贝成 {@link BatchFormulaSnapshot} 落库，
 * 之后展示标准参数、推进超差判定都只用快照，配方再发布 / 停用不影响在产批次。
 */
public final class FormulaSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private FormulaSupport() {
    }

    /** 设备类型字典（与 machine.machine_type 对齐） */
    public static final List<String> MACHINE_TYPES = List.of("WASHER", "DRYER", "IRONER");

    public static String machineTypeName(String code) {
        return switch (code) {
            case "WASHER" -> "水洗机";
            case "DRYER" -> "烘干机";
            case "IRONER" -> "烫平机";
            default -> code;
        };
    }

    public static List<String> splitMachineTypes(String csv) {
        List<String> types = new ArrayList<>();
        if (csv == null || csv.isBlank()) {
            return types;
        }
        for (String t : csv.split(",")) {
            String v = t.trim();
            if (!v.isEmpty() && !types.contains(v)) {
                types.add(v);
            }
        }
        return types;
    }

    public static String joinMachineTypes(List<String> types) {
        if (types == null || types.isEmpty()) {
            return "";
        }
        return String.join(",", types.stream().map(String::trim).filter(s -> !s.isEmpty()).distinct().toList());
    }

    /** 把已发布配方深拷贝为批次快照 */
    public static BatchFormulaSnapshot toSnapshot(FormulaVersion version) {
        BatchFormulaSnapshot snapshot = new BatchFormulaSnapshot();
        snapshot.setVersionId(version.getId());
        snapshot.setVersionNo(version.getVersionNo());
        snapshot.setCategoryId(version.getCategoryId());
        snapshot.setMachineTypes(splitMachineTypes(version.getMachineTypes()));
        snapshot.setMaxLoadRatio(version.getMaxLoadRatio());
        snapshot.setTempTolerance(version.getTempTolerance());
        snapshot.setDetergentTolerance(version.getDetergentTolerance());
        List<BatchFormulaSnapshot.Step> steps = new ArrayList<>();
        if (version.getSteps() != null) {
            for (FormulaStepParam p : version.getSteps()) {
                BatchFormulaSnapshot.Step step = new BatchFormulaSnapshot.Step();
                step.setStepCode(p.getStepCode());
                step.setStepName(p.getStepName());
                step.setStepOrder(p.getStepOrder());
                step.setTargetTemp(p.getTargetTemp());
                step.setTargetDetergentMl(p.getTargetDetergentMl());
                steps.add(step);
            }
        }
        snapshot.setSteps(steps);
        return snapshot;
    }

    public static String writeJson(BatchFormulaSnapshot snapshot) {
        try {
            return MAPPER.writeValueAsString(snapshot);
        } catch (Exception e) {
            throw new BizException("配方快照序列化失败：" + e.getMessage());
        }
    }

    public static BatchFormulaSnapshot readJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, BatchFormulaSnapshot.class);
        } catch (Exception e) {
            throw new BizException("批次配方快照解析失败：" + e.getMessage());
        }
    }

    /**
     * 判定某道工序实际值是否超出配方快照容差。
     * 返回 null 表示合格；否则返回中文超差说明。
     * 水温：|实际-目标| ≥ 容差即超差（容差是严格上界）；
     * 洗涤剂：目标为 0 的工序不投洗涤剂，实际 >0 即超差；否则偏差百分比 ≥ 容差% 超差。
     */
    public static String deviation(BatchFormulaSnapshot snapshot, BatchFormulaSnapshot.Step std,
                                   int actualTemp, int actualDetergent) {
        List<String> hits = new ArrayList<>();
        int tempTol = snapshot.getTempTolerance() == null ? 0 : snapshot.getTempTolerance();
        int targetTemp = std == null || std.getTargetTemp() == null ? 0 : std.getTargetTemp();
        if (Math.abs(actualTemp - targetTemp) > tempTol) {
            hits.add("实际水温 " + actualTemp + "℃，配方目标 " + targetTemp + "±" + tempTol + "℃");
        }

        BigDecimal detTol = snapshot.getDetergentTolerance() == null
                ? BigDecimal.ZERO : snapshot.getDetergentTolerance();
        int targetDet = std == null || std.getTargetDetergentMl() == null ? 0 : std.getTargetDetergentMl();
        boolean detOver;
        if (targetDet == 0) {
            detOver = actualDetergent > 0;
        } else {
            BigDecimal diffPct = BigDecimal.valueOf(Math.abs(actualDetergent - targetDet))
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(targetDet), 2, RoundingMode.HALF_UP);
            detOver = diffPct.compareTo(detTol) > 0;
        }
        if (detOver) {
            hits.add("实际洗涤剂 " + actualDetergent + "ml，配方目标 " + targetDet
                    + "ml（容差 ±" + detTol.stripTrailingZeros().toPlainString() + "%）");
        }
        return hits.isEmpty() ? null : String.join("；", hits);
    }
}
