package com.example.achievement.enums;

public enum ChangeType {
    STATUS_CHANGE("status_change", "状态变更"),
    VERSION_CHANGE("version_change", "版本变更"),
    FIELD_UPDATE("field_update", "字段更新");

    private final String code;
    private final String description;

    ChangeType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
