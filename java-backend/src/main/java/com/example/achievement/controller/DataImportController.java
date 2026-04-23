package com.example.achievement.controller;

import com.example.achievement.dto.request.FilePathRequest;
import com.example.achievement.dto.response.ImportResult;
import com.example.achievement.service.DataImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 数据导入控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/data")
@Tag(name = "数据导入", description = "数据导入相关接口")
@RequiredArgsConstructor
public class DataImportController {

    private final DataImportService dataImportService;

    @PostMapping("/import")
    @Operation(summary = "上传Excel文件并导入数据")
    public ResponseEntity<ImportResult> importFromUpload(
            @Parameter(description = "Excel文件") @RequestParam("file") MultipartFile file) {

        log.info("接收到文件上传导入请求: {}", file.getOriginalFilename());

        // 验证文件
        if (file.isEmpty()) {
            ImportResult result = ImportResult.builder()
                    .totalRecords(0)
                    .successCount(0)
                    .failedCount(0)
                    .skippedCount(0)
                    .build();
            result.addError("上传的文件为空");
            return ResponseEntity.badRequest().body(result);
        }

        // 验证文件类型
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            ImportResult result = ImportResult.builder()
                    .totalRecords(0)
                    .successCount(0)
                    .failedCount(0)
                    .skippedCount(0)
                    .build();
            result.addError("文件格式不正确，仅支持.xlsx或.xls格式的Excel文件");
            return ResponseEntity.badRequest().body(result);
        }

        try {
            ImportResult result = dataImportService.importFromExcel(file.getInputStream());
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            log.error("读取上传文件失败", e);
            ImportResult result = ImportResult.builder()
                    .totalRecords(0)
                    .successCount(0)
                    .failedCount(0)
                    .skippedCount(0)
                    .build();
            result.addError("读取文件失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/import-file")
    @Operation(summary = "从指定文件路径导入数据")
    public ResponseEntity<ImportResult> importFromFile(
            @RequestBody FilePathRequest request) {

        String filePath = request.getFilePath();
        log.info("接收到文件路径导入请求: {}", filePath);

        // 验证文件路径
        if (filePath == null || filePath.trim().isEmpty()) {
            ImportResult result = ImportResult.builder()
                    .totalRecords(0)
                    .successCount(0)
                    .failedCount(0)
                    .skippedCount(0)
                    .build();
            result.addError("文件路径不能为空");
            return ResponseEntity.badRequest().body(result);
        }

        // 验证文件类型
        if (!filePath.endsWith(".xlsx") && !filePath.endsWith(".xls")) {
            ImportResult result = ImportResult.builder()
                    .totalRecords(0)
                    .successCount(0)
                    .failedCount(0)
                    .skippedCount(0)
                    .build();
            result.addError("文件格式不正确，仅支持.xlsx或.xls格式的Excel文件");
            return ResponseEntity.badRequest().body(result);
        }

        try {
            ImportResult result = dataImportService.importFromExcel(filePath);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("导入文件失败: {}", filePath, e);
            ImportResult result = ImportResult.builder()
                    .totalRecords(0)
                    .successCount(0)
                    .failedCount(0)
                    .skippedCount(0)
                    .build();
            result.addError("导入文件失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/reinitialize")
    @Operation(summary = "清空数据库并重新导入Excel数据")
    public ResponseEntity<ImportResult> reinitialize(
            @Parameter(description = "Excel文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "确认执行") @RequestParam(value = "confirm", defaultValue = "false") boolean confirm) {

        log.info("接收到重新初始化请求: {}, confirm={}", file.getOriginalFilename(), confirm);

        // 需要确认才能执行
        if (!confirm) {
            ImportResult result = ImportResult.builder()
                    .totalRecords(0)
                    .successCount(0)
                    .failedCount(0)
                    .skippedCount(0)
                    .build();
            result.addError("请设置 confirm=true 参数以确认执行重新初始化操作。此操作将清空所有数据！");
            return ResponseEntity.badRequest().body(result);
        }

        // 验证文件
        if (file.isEmpty()) {
            ImportResult result = ImportResult.builder()
                    .totalRecords(0)
                    .successCount(0)
                    .failedCount(0)
                    .skippedCount(0)
                    .build();
            result.addError("上传的文件为空");
            return ResponseEntity.badRequest().body(result);
        }

        // 验证文件类型
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            ImportResult result = ImportResult.builder()
                    .totalRecords(0)
                    .successCount(0)
                    .failedCount(0)
                    .skippedCount(0)
                    .build();
            result.addError("文件格式不正确，仅支持.xlsx或.xls格式的Excel文件");
            return ResponseEntity.badRequest().body(result);
        }

        try {
            ImportResult result = dataImportService.reinitializeFromExcel(file.getInputStream());
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            log.error("读取上传文件失败", e);
            ImportResult result = ImportResult.builder()
                    .totalRecords(0)
                    .successCount(0)
                    .failedCount(0)
                    .skippedCount(0)
                    .build();
            result.addError("读取文件失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
}
