package com.example.achievement.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Excel数据读取工具类
 * 用于读取Excel文件并解析"产品成果明细登记薄" sheet
 */
public class ExcelReaderUtil {

    private static final Logger logger = LoggerFactory.getLogger(ExcelReaderUtil.class);
    private static final String TARGET_SHEET_NAME = "产品成果明细登记薄";
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * 读取Excel文件并解析指定sheet
     *
     * @param filePath Excel文件路径
     * @return 解析后的数据列表，每行数据为一个Map
     * @throws IOException 文件读取异常
     */
    public static List<Map<String, String>> readExcel(String filePath) throws IOException {
        return readExcel(filePath, TARGET_SHEET_NAME);
    }

    /**
     * 读取Excel文件并解析指定sheet
     *
     * @param filePath  Excel文件路径
     * @param sheetName sheet名称
     * @return 解析后的数据列表，每行数据为一个Map
     * @throws IOException 文件读取异常
     */
    public static List<Map<String, String>> readExcel(String filePath, String sheetName) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("文件不存在: " + filePath);
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            return readExcel(fis, sheetName);
        }
    }

    /**
     * 从输入流读取Excel文件并解析指定sheet
     *
     * @param inputStream Excel文件输入流
     * @param sheetName   sheet名称
     * @return 解析后的数据列表，每行数据为一个Map
     * @throws IOException 文件读取异常
     */
    public static List<Map<String, String>> readExcel(InputStream inputStream, String sheetName) throws IOException {
        List<Map<String, String>> dataList = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                // 尝试获取第一个sheet
                sheet = workbook.getSheetAt(0);
                if (sheet == null) {
                    throw new IOException("未找到指定的sheet: " + sheetName);
                }
                logger.warn("未找到sheet [{}], 使用第一个sheet: {}", sheetName, sheet.getSheetName());
            }

            // 获取表头行
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IOException("Excel文件为空或格式不正确");
            }

            // 解析表头
            List<String> headers = parseHeader(headerRow);
            logger.info("Excel表头: {}", headers);

            // 从第二行开始解析数据
            int lastRowNum = sheet.getLastRowNum();
            for (int i = 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                // 跳过空行
                if (isRowEmpty(row)) {
                    continue;
                }

                Map<String, String> rowData = parseRow(row, headers);
                if (!rowData.isEmpty()) {
                    dataList.add(rowData);
                }
            }

            logger.info("成功解析Excel数据, 共{}行", dataList.size());
        }

        return dataList;
    }

    /**
     * 解析表头行
     *
     * @param headerRow 表头行
     * @return 表头列表
     */
    private static List<String> parseHeader(Row headerRow) {
        List<String> headers = new ArrayList<>();
        int lastCellNum = headerRow.getLastCellNum();

        for (int i = 0; i < lastCellNum; i++) {
            Cell cell = headerRow.getCell(i);
            String headerValue = getCellValueAsString(cell);
            headers.add(headerValue != null ? headerValue.trim() : "column_" + i);
        }

        return headers;
    }

    /**
     * 解析数据行
     *
     * @param row     数据行
     * @param headers 表头列表
     * @return 行数据Map
     */
    private static Map<String, String> parseRow(Row row, List<String> headers) {
        Map<String, String> rowData = new LinkedHashMap<>();
        int lastCellNum = row.getLastCellNum();

        for (int i = 0; i < headers.size() && i < lastCellNum; i++) {
            Cell cell = row.getCell(i);
            String cellValue = getCellValueAsString(cell);
            rowData.put(headers.get(i), cellValue);
        }

        return rowData;
    }

    /**
     * 获取单元格的字符串值
     *
     * @param cell 单元格
     * @return 字符串值
     */
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();

            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    return DATE_FORMAT.format(date);
                } else {
                    double numericValue = cell.getNumericCellValue();
                    // 如果是整数，返回整数格式
                    if (numericValue == Math.floor(numericValue)) {
                        return String.valueOf((long) numericValue);
                    }
                    return DECIMAL_FORMAT.format(numericValue);
                }

            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());

            case FORMULA:
                return getFormulaCellValue(cell);

            case BLANK:
                return "";

            default:
                return "";
        }
    }

    /**
     * 获取公式单元格的值
     *
     * @param cell 公式单元格
     * @return 字符串值
     */
    private static String getFormulaCellValue(Cell cell) {
        try {
            switch (cell.getCachedFormulaResultType()) {
                case NUMERIC:
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == Math.floor(numericValue)) {
                        return String.valueOf((long) numericValue);
                    }
                    return DECIMAL_FORMAT.format(numericValue);

                case STRING:
                    return cell.getStringCellValue().trim();

                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());

                default:
                    return "";
            }
        } catch (Exception e) {
            logger.warn("获取公式单元格值失败: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 判断行是否为空
     *
     * @param row 数据行
     * @return 是否为空行
     */
    private static boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }

        int lastCellNum = row.getLastCellNum();
        for (int i = 0; i < lastCellNum; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellValueAsString(cell);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 获取Excel文件中的所有sheet名称
     *
     * @param filePath Excel文件路径
     * @return sheet名称列表
     * @throws IOException 文件读取异常
     */
    public static List<String> getSheetNames(String filePath) throws IOException {
        List<String> sheetNames = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            int numberOfSheets = workbook.getNumberOfSheets();
            for (int i = 0; i < numberOfSheets; i++) {
                sheetNames.add(workbook.getSheetName(i));
            }
        }

        return sheetNames;
    }
}
