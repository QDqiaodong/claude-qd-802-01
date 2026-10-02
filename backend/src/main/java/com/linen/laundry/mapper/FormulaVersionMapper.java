package com.linen.laundry.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Many;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.linen.laundry.domain.FormulaStepParam;
import com.linen.laundry.domain.FormulaVersion;

/**
 * 洗涤配方版本：查询带出八道工序参数（嵌套 @Many）与品类信息。
 * 「每品类至多一个 PUBLISHED」由表上 UNIQUE(category_id, current_flag) 生成列唯一索引兜底。
 */
@Mapper
public interface FormulaVersionMapper {

    @Select("""
            SELECT v.*, c.name AS category_name, c.code AS category_code,
                   cf.version_no AS copy_from_version_no,
                   (c.current_formula_version_id = v.id) AS is_current
              FROM formula_version v
              JOIN linen_category c ON c.id = v.category_id
              LEFT JOIN formula_version cf ON cf.id = v.copy_from_id
             ORDER BY v.category_id, v.version_seq DESC, v.id DESC
            """)
    @Results(id = "formulaMap", value = {
            @Result(property = "id", column = "id", id = true),
            @Result(property = "versionNo", column = "version_no"),
            @Result(property = "categoryId", column = "category_id"),
            @Result(property = "versionSeq", column = "version_seq"),
            @Result(property = "status", column = "status"),
            @Result(property = "machineTypes", column = "machine_types"),
            @Result(property = "maxLoadRatio", column = "max_load_ratio"),
            @Result(property = "tempTolerance", column = "temp_tolerance"),
            @Result(property = "detergentTolerance", column = "detergent_tolerance"),
            @Result(property = "copyFromId", column = "copy_from_id"),
            @Result(property = "createdBy", column = "created_by"),
            @Result(property = "publishedBy", column = "published_by"),
            @Result(property = "publishedAt", column = "published_at"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "categoryName", column = "category_name"),
            @Result(property = "categoryCode", column = "category_code"),
            @Result(property = "copyFromVersionNo", column = "copy_from_version_no"),
            @Result(property = "current", column = "is_current"),
            @Result(property = "steps", column = "id",
                    many = @Many(select = "com.linen.laundry.mapper.FormulaVersionMapper.selectSteps"))
    })
    List<FormulaVersion> selectAll();

    @Select("""
            SELECT v.*, c.name AS category_name, c.code AS category_code,
                   cf.version_no AS copy_from_version_no,
                   (c.current_formula_version_id = v.id) AS is_current
              FROM formula_version v
              JOIN linen_category c ON c.id = v.category_id
              LEFT JOIN formula_version cf ON cf.id = v.copy_from_id
             WHERE v.category_id = #{categoryId}
             ORDER BY v.version_seq DESC, v.id DESC
            """)
    @ResultMap("formulaMap")
    List<FormulaVersion> selectByCategoryId(@Param("categoryId") Long categoryId);

    @Select("""
            SELECT v.*, c.name AS category_name, c.code AS category_code,
                   cf.version_no AS copy_from_version_no,
                   (c.current_formula_version_id = v.id) AS is_current
              FROM formula_version v
              JOIN linen_category c ON c.id = v.category_id
              LEFT JOIN formula_version cf ON cf.id = v.copy_from_id
             WHERE v.id = #{id}
            """)
    @ResultMap("formulaMap")
    FormulaVersion selectById(@Param("id") Long id);

    /** 不带关联的行锁查询，供发布 / 停用在事务内重检状态 */
    @Select("SELECT * FROM formula_version WHERE id = #{id} FOR UPDATE")
    FormulaVersion selectByIdForUpdate(@Param("id") Long id);

    /** 品类当前生效版本（status=PUBLISHED 已由唯一索引保证至多一条） */
    @Select("SELECT * FROM formula_version WHERE category_id = #{categoryId} AND status = 'PUBLISHED' LIMIT 1")
    @ResultMap("formulaMap")
    FormulaVersion selectCurrent(@Param("categoryId") Long categoryId);

    /** 嵌套 @Many：八道工序参数 */
    @Select("SELECT * FROM formula_step_param WHERE version_id = #{versionId} ORDER BY step_order")
    List<FormulaStepParam> selectSteps(@Param("versionId") Long versionId);

    /** 该品类下一个版本序号 */
    @Select("SELECT COALESCE(MAX(version_seq), 0) + 1 FROM formula_version WHERE category_id = #{categoryId}")
    int nextVersionSeq(@Param("categoryId") Long categoryId);

    @Insert("""
            INSERT INTO formula_version (version_no, category_id, version_seq, status, machine_types,
                                         max_load_ratio, temp_tolerance, detergent_tolerance, copy_from_id,
                                         created_by, published_by, published_at, remark)
            VALUES (#{versionNo}, #{categoryId}, #{versionSeq}, #{status}, #{machineTypes},
                    #{maxLoadRatio}, #{tempTolerance}, #{detergentTolerance}, #{copyFromId},
                    #{createdBy}, #{publishedBy}, #{publishedAt}, #{remark})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(FormulaVersion version);

    /** 草稿调整：仅草稿内容列可被覆盖（service 层先校验 status=DRAFT） */
    @Update("""
            UPDATE formula_version
               SET machine_types = #{machineTypes}, max_load_ratio = #{maxLoadRatio},
                   temp_tolerance = #{tempTolerance}, detergent_tolerance = #{detergentTolerance},
                   remark = #{remark}, created_by = #{createdBy}
             WHERE id = #{id} AND status = 'DRAFT'
            """)
    int updateDraft(FormulaVersion version);

    /**
     * 发布：DRAFT → PUBLISHED。先在事务内把同品类旧 PUBLISHED 置 ARCHIVED，
     * 唯一索引 (category_id, current_flag) 兜底并发，CAS 更新 0 行时 service 抛明确冲突。
     */
    @Update("UPDATE formula_version SET status = 'PUBLISHED', published_by = #{publishedBy}, "
            + "published_at = #{publishedAt} WHERE id = #{id} AND status = 'DRAFT'")
    int markPublished(@Param("id") Long id,
                      @Param("publishedBy") String publishedBy,
                      @Param("publishedAt") LocalDateTime publishedAt);

    /** 旧当前版本归档（被新版本接替 / 停用切换） */
    @Update("UPDATE formula_version SET status = 'ARCHIVED' WHERE id = #{id} AND status = 'PUBLISHED'")
    int markArchived(@Param("id") Long id);

    @Delete("DELETE FROM formula_step_param WHERE version_id = #{versionId}")
    int deleteSteps(@Param("versionId") Long versionId);

    /** 仅草稿可删除（service 层校验）；工序参数由调用方先清 */
    @Delete("DELETE FROM formula_version WHERE id = #{id} AND status = 'DRAFT'")
    int deleteDraft(@Param("id") Long id);

    @Insert("""
            INSERT INTO formula_step_param (version_id, step_code, step_name, step_order,
                                            target_temp, target_detergent_ml)
            VALUES (#{versionId}, #{stepCode}, #{stepName}, #{stepOrder},
                    #{targetTemp}, #{targetDetergentMl})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertStep(FormulaStepParam step);
}
