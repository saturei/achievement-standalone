package com.example.achievement.service;

import com.example.achievement.config.DingTalkProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DingTalkClient {

    private final DingTalkAuthService authService;
    private final DingTalkProperties props;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String BASE_URL = "https://api.dingtalk.com/v1.0/notable";

    public List<Map<String, Object>> listAllRecords(String sheetName) {
        List<Map<String, Object>> allRecords = new ArrayList<>();
        String nextToken = null;

        do {
            Map<String, Object> page = fetchPage(sheetName, nextToken);
            if (page == null) break;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> records = (List<Map<String, Object>>) page.get("records");
            if (records != null) {
                allRecords.addAll(records);
                log.info("钉钉 sheet={} 读取 {} 条，累计 {} 条", sheetName, records.size(), allRecords.size());
            }

            Boolean hasMore = (Boolean) page.get("hasMore");
            nextToken = (String) page.get("nextToken");
            if (!Boolean.TRUE.equals(hasMore) || nextToken == null || nextToken.isEmpty()) {
                break;
            }
        } while (true);

        log.info("钉钉 sheet={} 全部读取完成，共 {} 条", sheetName, allRecords.size());
        return allRecords;
    }

    private Map<String, Object> fetchPage(String sheetName, String nextToken) {
        try {
            String token = authService.getAccessToken();
            String url = BASE_URL + "/bases/" + props.getBaseId() + "/sheets/" + sheetName + "/records/list";
            if (props.getOperatorId() != null && !props.getOperatorId().isEmpty()) {
                url += "?operatorId=" + props.getOperatorId();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-acs-dingtalk-access-token", token);
            if (props.getOperatorId() != null && !props.getOperatorId().isEmpty()) {
                headers.set("x-acs-dingtalk-operatorId", props.getOperatorId());
            }

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("maxResults", 100);
            if (nextToken != null && !nextToken.isEmpty()) {
                body.put("nextToken", nextToken);
            }

            ResponseEntity<Map> resp = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(body, headers), Map.class);

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                return resp.getBody();
            } else {
                log.error("钉钉API调用失败: {} {}", resp.getStatusCode(), resp.getBody());
                return null;
            }
        } catch (Exception e) {
            log.error("钉钉API调用异常: {}", e.getMessage());
            return null;
        }
    }

    public String getFieldValue(Map<String, Object> record, String fieldName) {
        @SuppressWarnings("unchecked")
        Map<String, Object> fields = (Map<String, Object>) record.get("fields");
        if (fields == null) return null;
        Object val = fields.get(fieldName);
        if (val == null) return null;
        if (val instanceof List && !((List<?>) val).isEmpty()) {
            return String.valueOf(((List<?>) val).get(0));
        }
        return String.valueOf(val);
    }

    public String getFieldValue(Map<String, Object> record, String fieldName, String defaultValue) {
        String val = getFieldValue(record, fieldName);
        return val != null ? val : defaultValue;
    }
}
