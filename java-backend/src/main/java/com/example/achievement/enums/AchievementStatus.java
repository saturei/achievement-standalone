package com.example.achievement.enums;

public enum AchievementStatus {
    PRE_REGISTER("pre_register", "预注册"),
    REGISTER("register", "注册"),
    RECORDED("recorded", "登记"),
    OFFLINE("offline", "下架"),
    DELETED("deleted", "已删除");

    private final String code;
    private final String description;

    AchievementStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static AchievementStatus fromCode(String code) {
        for (AchievementStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status code: " + code);
    }
}
