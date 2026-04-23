package com.example.achievement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据导入结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResult {

    /**
     * 总记录数
     */
    private int totalRecords;

    /**
     * 成功导入数量
     */
    private int successCount;

    /**
     * 失败数量
     */
    private int failedCount;

    /**
     * 跳过数量（已存在的记录）
     */
    private int skippedCount;

    /**
     * 错误信息列表
     */
    @Builder.Default
    private List<String> errors = new ArrayList<>();

    /**
     * 警告信息列表
     */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    /**
     * 成功导入的成果ID列表
     */
    @Builder.Default
    private List<String> importedIds = new ArrayList<>();

    /**
     * 添加错误信息
     */
    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
    }

    /**
     * 添加警告信息
     */
    public void addWarning(String warning) {
        if (this.warnings == null) {
            this.warnings = new ArrayList<>();
        }
        this.warnings.add(warning);
    }

    /**
     * 添加导入成功的ID
     */
    public void addImportedId(String id) {
        if (this.importedIds == null) {
            this.importedIds = new ArrayList<>();
        }
        this.importedIds.add(id);
    }
}
