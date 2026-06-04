package com.example.achievement.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "dingtalk")
public class DingTalkProperties {
    private String corpId;
    private String appKey;
    private String appSecret;
    private String baseId;
    private String dataWarehouseBaseId;
    private String operatorId;
    private String sheetSignings = "签约明细";
    private String sheetRecognitions = "确权明细";

    public boolean isConfigured() {
        return appKey != null && !appKey.isEmpty()
                && appSecret != null && !appSecret.isEmpty()
                && baseId != null && !baseId.isEmpty();
    }
}
