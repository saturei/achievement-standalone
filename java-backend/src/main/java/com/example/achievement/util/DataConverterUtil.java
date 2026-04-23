package com.example.achievement.util;

import com.example.achievement.enums.AchievementStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * 数据转换工具类
 * 用于Excel数据导入时的数据格式转换
 */
public class DataConverterUtil {

    private static final Logger logger = LoggerFactory.getLogger(DataConverterUtil.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // 日期格式化模式
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy年MM月"),
            DateTimeFormatter.ofPattern("yyyy-MM"),
            DateTimeFormatter.ofPattern("yyyy/MM"),
            DateTimeFormatter.ofPattern("yyyy.MM"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy年MM月dd日")
    };

    // 状态映射表（中文 -> 枚举）
    private static final Map<String, AchievementStatus> STATUS_MAPPING = new HashMap<>();

    static {
        STATUS_MAPPING.put("预注册", AchievementStatus.PRE_REGISTER);
        STATUS_MAPPING.put("注册", AchievementStatus.REGISTER);
        STATUS_MAPPING.put("登记", AchievementStatus.RECORDED);
        STATUS_MAPPING.put("下架", AchievementStatus.OFFLINE);
        STATUS_MAPPING.put("已删除", AchievementStatus.DELETED);
        STATUS_MAPPING.put("pre_register", AchievementStatus.PRE_REGISTER);
        STATUS_MAPPING.put("register", AchievementStatus.REGISTER);
        STATUS_MAPPING.put("recorded", AchievementStatus.RECORDED);
        STATUS_MAPPING.put("offline", AchievementStatus.OFFLINE);
        STATUS_MAPPING.put("deleted", AchievementStatus.DELETED);
    }

    /**
     * 转换日期字符串为LocalDate
     * 支持多种日期格式，如"2026年01月"、"2026-01"、"2026/01"等
     *
     * @param dateStr 日期字符串
     * @return LocalDate对象，转换失败返回null
     */
    public static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        String trimmedDate = dateStr.trim();

        // 尝试各种日期格式
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                // 对于只有年月的格式，默认设置为当月第一天
                if (formatter.toString().contains("MM月") || 
                    formatter.toString().contains("MM") && !formatter.toString().contains("dd")) {
                    // 解析年月格式，返回当月第一天
                    try {
                        Map<String, Integer> yearMonth = parseYearMonth(trimmedDate);
                        if (yearMonth != null) {
                            return LocalDate.of(yearMonth.get("year"), yearMonth.get("month"), 1);
                        }
                    } catch (Exception ignored) {
                    }
                }
                return LocalDate.parse(trimmedDate, formatter);
            } catch (DateTimeParseException ignored) {
                // 继续尝试下一个格式
            }
        }

        // 尝试提取年月
        Map<String, Integer> yearMonth = parseYearMonth(trimmedDate);
        if (yearMonth != null) {
            return LocalDate.of(yearMonth.get("year"), yearMonth.get("month"), 1);
        }

        logger.warn("无法解析日期: {}", dateStr);
        return null;
    }

    /**
     * 从字符串中提取年月
     *
     * @param dateStr 日期字符串
     * @return 包含year和month的Map，解析失败返回null
     */
    private static Map<String, Integer> parseYearMonth(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        try {
            // 匹配 "2026年01月" 格式
            if (dateStr.contains("年") && dateStr.contains("月")) {
                String normalized = dateStr.replace("年", "-").replace("月", "");
                String[] parts = normalized.split("-");
                if (parts.length >= 2) {
                    int year = Integer.parseInt(parts[0].trim());
                    int month = Integer.parseInt(parts[1].trim());
                    if (month >= 1 && month <= 12) {
                        Map<String, Integer> result = new HashMap<>();
                        result.put("year", year);
                        result.put("month", month);
                        return result;
                    }
                }
            }

            // 匹配 "2026-01" 或 "2026/01" 格式
            String[] separators = {"-", "/", "."};
            for (String sep : separators) {
                if (dateStr.contains(sep)) {
                    String[] parts = dateStr.split(sep);
                    if (parts.length >= 2) {
                        int year = Integer.parseInt(parts[0].trim());
                        int month = Integer.parseInt(parts[1].trim());
                        if (month >= 1 && month <= 12) {
                            Map<String, Integer> result = new HashMap<>();
                            result.put("year", year);
                            result.put("month", month);
                            return result;
                        }
                    }
                }
            }
        } catch (NumberFormatException e) {
            logger.debug("解析年月失败: {}", dateStr);
        }

        return null;
    }

    /**
     * 转换状态字符串为枚举
     * 支持中文和英文状态名称
     *
     * @param statusStr 状态字符串
     * @return 状态枚举，转换失败返回null
     */
    public static AchievementStatus parseStatus(String statusStr) {
        if (statusStr == null || statusStr.trim().isEmpty()) {
            return null;
        }

        String trimmedStatus = statusStr.trim();
        AchievementStatus status = STATUS_MAPPING.get(trimmedStatus);

        if (status == null) {
            // 尝试忽略大小写匹配
            for (Map.Entry<String, AchievementStatus> entry : STATUS_MAPPING.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(trimmedStatus)) {
                    return entry.getValue();
                }
            }
            logger.warn("无法识别的状态: {}", statusStr);
        }

        return status;
    }

    /**
     * 转换状态字符串为枚举代码
     *
     * @param statusStr 状态字符串
     * @return 状态枚举代码，转换失败返回原字符串
     */
    public static String parseStatusCode(String statusStr) {
        AchievementStatus status = parseStatus(statusStr);
        return status != null ? status.getCode() : statusStr;
    }

    /**
     * 将套餐ID列表转换为JSON字符串
     *
     * @param packageIds 套餐ID列表（逗号分隔的字符串或列表）
     * @return JSON字符串
     */
    public static String convertPackageIdsToJson(String packageIds) {
        if (packageIds == null || packageIds.trim().isEmpty()) {
            return "[]";
        }

        // 如果已经是JSON格式，直接返回
        String trimmed = packageIds.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            return trimmed;
        }

        // 将逗号分隔的字符串转换为JSON数组
        List<String> idList = parseCommaSeparatedValues(trimmed);
        return convertListToJson(idList);
    }

    /**
     * 将套餐ID列表转换为JSON字符串
     *
     * @param packageIds 套餐ID列表
     * @return JSON字符串
     */
    public static String convertPackageIdsToJson(List<String> packageIds) {
        if (packageIds == null || packageIds.isEmpty()) {
            return "[]";
        }
        return convertListToJson(packageIds);
    }

    /**
     * 将列表转换为JSON字符串
     *
     * @param list 列表
     * @return JSON字符串
     */
    public static String convertListToJson(List<?> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            logger.error("转换列表为JSON失败", e);
            return "[]";
        }
    }

    /**
     * 将JSON字符串转换为列表
     *
     * @param jsonStr JSON字符串
     * @return 列表
     */
    public static List<String> convertJsonToList(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            return OBJECT_MAPPER.readValue(jsonStr, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            logger.error("转换JSON为列表失败: {}", jsonStr, e);
            return new ArrayList<>();
        }
    }

    /**
     * 解析逗号分隔的值
     *
     * @param value 逗号分隔的字符串
     * @return 值列表
     */
    public static List<String> parseCommaSeparatedValues(String value) {
        if (value == null || value.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();
        String[] parts = value.split("[,，;；\\s]+");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }

        return result;
    }

    /**
     * 安全地获取字符串值，处理null和空白
     *
     * @param value 原始值
     * @return 处理后的值，空白返回null
     */
    public static String safeString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    /**
     * 安全地获取整数值
     *
     * @param value 字符串值
     * @return 整数值，转换失败返回null
     */
    public static Integer safeInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            logger.debug("转换整数失败: {}", value);
            return null;
        }
    }

    /**
     * 安全地获取长整数值
     *
     * @param value 字符串值
     * @return 长整数值，转换失败返回null
     */
    public static Long safeLong(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            logger.debug("转换长整数失败: {}", value);
            return null;
        }
    }

    /**
     * 安全地获取布尔值
     *
     * @param value 字符串值
     * @return 布尔值
     */
    public static Boolean safeBoolean(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String lower = value.trim().toLowerCase();
        if ("true".equals(lower) || "是".equals(lower) || "yes".equals(lower) || "1".equals(lower)) {
            return true;
        }
        if ("false".equals(lower) || "否".equals(lower) || "no".equals(lower) || "0".equals(lower)) {
            return false;
        }

        return null;
    }

    /**
     * 将Map转换为JSON字符串
     *
     * @param map Map对象
     * @return JSON字符串
     */
    public static String convertMapToJson(Map<String, ?> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            logger.error("转换Map为JSON失败", e);
            return "{}";
        }
    }

    /**
     * 将JSON字符串转换为Map
     *
     * @param jsonStr JSON字符串
     * @return Map对象
     */
    public static Map<String, Object> convertJsonToMap(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return new HashMap<>();
        }

        try {
            return OBJECT_MAPPER.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            logger.error("转换JSON为Map失败: {}", jsonStr, e);
            return new HashMap<>();
        }
    }
}
