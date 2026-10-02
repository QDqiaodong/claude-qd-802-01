package com.linen.laundry.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.linen.laundry.domain.BatchFormulaReview;

/**
 * 批次参数超差复核：同一批次同一工序至多一条 PENDING（生成列唯一索引兜底）。
 */
@Mapper
public interface BatchFormulaReviewMapper {

    @Select("""
            SELECT r.*, b.batch_no AS batch_no, c.name AS category_name
              FROM batch_formula_review r
              JOIN wash_batch b ON b.id = r.batch_id
              JOIN linen_category c ON c.id = b.category_id
             ORDER BY FIELD(r.status, 'PENDING', 'PASS', 'REWORK'), r.id DESC
            """)
    @Results(id = "reviewMap", value = {
            @Result(property = "id", column = "id", id = true),
            @Result(property = "batchId", column = "batch_id"),
            @Result(property = "stepCode", column = "step_code"),
            @Result(property = "stepName", column = "step_name"),
            @Result(property = "stepOrder", column = "step_order"),
            @Result(property = "formulaVersionId", column = "formula_version_id"),
            @Result(property = "targetTemp", column = "target_temp"),
            @Result(property = "actualTemp", column = "actual_temp"),
            @Result(property = "targetDetergentMl", column = "target_detergent_ml"),
            @Result(property = "actualDetergentMl", column = "actual_detergent_ml"),
            @Result(property = "tempTolerance", column = "temp_tolerance"),
            @Result(property = "detergentTolerance", column = "detergent_tolerance"),
            @Result(property = "deviationDesc", column = "deviation_desc"),
            @Result(property = "status", column = "status"),
            @Result(property = "reviewReason", column = "review_reason"),
            @Result(property = "reviewer", column = "reviewer"),
            @Result(property = "reviewedAt", column = "reviewed_at"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "batchNo", column = "batch_no"),
            @Result(property = "categoryName", column = "category_name")
    })
    List<BatchFormulaReview> selectAll();

    @Select("""
            SELECT r.*, b.batch_no AS batch_no, c.name AS category_name
              FROM batch_formula_review r
              JOIN wash_batch b ON b.id = r.batch_id
              JOIN linen_category c ON c.id = b.category_id
             WHERE r.id = #{id}
            """)
    @ResultMap("reviewMap")
    BatchFormulaReview selectById(@Param("id") Long id);

    /** 批次当前待复核记录（至多一条） */
    @Select("""
            SELECT r.*, b.batch_no AS batch_no, c.name AS category_name
              FROM batch_formula_review r
              JOIN wash_batch b ON b.id = r.batch_id
              JOIN linen_category c ON c.id = b.category_id
             WHERE r.batch_id = #{batchId} AND r.status = 'PENDING'
             ORDER BY r.id DESC LIMIT 1
            """)
    @ResultMap("reviewMap")
    BatchFormulaReview selectPendingByBatchId(@Param("batchId") Long batchId);

    @Select("SELECT COUNT(*) FROM batch_formula_review WHERE status = 'PENDING'")
    int countPending();

    @Insert("""
            INSERT INTO batch_formula_review (batch_id, step_code, step_name, step_order, formula_version_id,
                                              target_temp, actual_temp, target_detergent_ml, actual_detergent_ml,
                                              temp_tolerance, detergent_tolerance, deviation_desc, status)
            VALUES (#{batchId}, #{stepCode}, #{stepName}, #{stepOrder}, #{formulaVersionId},
                    #{targetTemp}, #{actualTemp}, #{targetDetergentMl}, #{actualDetergentMl},
                    #{tempTolerance}, #{detergentTolerance}, #{deviationDesc}, 'PENDING')
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(BatchFormulaReview review);

    /** 主管复核：PENDING → PASS / REWORK（CAS，防止重复处理） */
    @Update("""
            UPDATE batch_formula_review
               SET status = #{decision}, review_reason = #{reason},
                   reviewer = #{reviewer}, reviewed_at = #{reviewedAt}
             WHERE id = #{id} AND status = 'PENDING'
            """)
    int decide(@Param("id") Long id,
               @Param("decision") String decision,
               @Param("reason") String reason,
               @Param("reviewer") String reviewer,
               @Param("reviewedAt") LocalDateTime reviewedAt);
}
