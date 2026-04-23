package com.example.achievement.service;

import com.example.achievement.dto.response.ImportResult;

import java.io.InputStream;

/**
 * 数据导入服务接口
 */
public interface DataImportService {

    /**
     * 从Excel文件导入数据
     *
     * @param filePath Excel文件路径
     * @return 导入结果
     */
    ImportResult importFromExcel(String filePath);

    /**
     * 从输入流导入数据
     *
     * @param inputStream Excel文件输入流
     * @return 导入结果
     */
    ImportResult importFromExcel(InputStream inputStream);

    /**
     * 清空数据库并重新导入Excel数据
     *
     * @param inputStream Excel文件输入流
     * @return 导入结果
     */
    ImportResult reinitializeFromExcel(InputStream inputStream);
}
