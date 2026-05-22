package com.example.achievement.service;

import com.example.achievement.config.DingTalkProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DingTalkAuthService {

    private final DingTalkProperties props;

    private String accessToken;
    private long expireAt;

    private static final int REFRESH_BEFORE_SECONDS = 300;
    private static final String TOKEN_URL = "https://api.dingtalk.com/v1.0/oauth2/accessToken";

    @EventListener(ApplicationStartedEvent.class)
    public void onStart() {
        if (props.isConfigured()) {
            refreshToken();
        }
    }

    public String getAccessToken() {
        if (!props.isConfigured()) {
            throw new IllegalStateException("钉钉未配置");
        }
        if (accessToken == null || isExpiringSoon()) {
            refreshToken();
        }
        return accessToken;
    }

    private boolean isExpiringSoon() {
        return Instant.now().getEpochSecond() + REFRESH_BEFORE_SECONDS >= expireAt;
    }

    private synchronized void refreshToken() {
        try {
            RestTemplate rt = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new LinkedHashMap<>();
            body.put("appKey", props.getAppKey());
            body.put("appSecret", props.getAppSecret());

            ResponseEntity<Map> resp = rt.postForEntity(TOKEN_URL,
                    new HttpEntity<>(body, headers), Map.class);

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                this.accessToken = (String) resp.getBody().get("accessToken");
                Object expireIn = resp.getBody().get("expireIn");
                long expireSeconds = expireIn instanceof Number ? ((Number) expireIn).longValue() : 7200;
                this.expireAt = Instant.now().getEpochSecond() + expireSeconds;
                log.info("钉钉 access_token 获取成功, 有效期 {} 秒", expireSeconds);
            } else {
                log.error("钉钉 access_token 获取失败: {}", resp.getStatusCode());
            }
        } catch (Exception e) {
            log.error("钉钉 access_token 获取异常: {}", e.getMessage());
        }
    }
}
