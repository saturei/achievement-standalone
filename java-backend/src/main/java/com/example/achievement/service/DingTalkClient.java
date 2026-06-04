package com.example.achievement.service;

import com.example.achievement.config.DingTalkProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class DingTalkClient {

    private final DingTalkAuthService authService;
    private final DingTalkProperties props;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String BASE_URL = "https://api.dingtalk.com/v1.0/notable";
    private static final int MAX_RESULTS = 100;

    private final AtomicInteger callCounter = new AtomicInteger(0);
    public static final int MONTHLY_LIMIT = 500;

    public int getCallCount() { return callCounter.get(); }
    public void resetCallCount() { callCounter.set(0); }
    public int remainingCalls() { return MONTHLY_LIMIT - callCounter.get(); }
    public boolean isNearLimit() { return callCounter.get() > MONTHLY_LIMIT * 0.85; }

    // ---- public: use configured baseId ----
    public List<Map<String, Object>> listAllRecords(String sheetId) {
        return listAllRecords(sheetId, props.getBaseId());
    }

    public List<Map<String, String>> listRecordsWithFieldNames(String sheetId) {
        return listRecordsWithFieldNames(sheetId, props.getBaseId());
    }

    // ---- public: use override baseId (for data warehouse) ----
    public List<Map<String, Object>> listAllRecords(String sheetId, String baseId) {
        List<Map<String, Object>> allRecords = new ArrayList<>();
        String nextToken = null;
        do {
            Map<String, Object> page = fetchPage(baseId, sheetId, nextToken);
            if (page == null) break;
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> records = (List<Map<String, Object>>) page.get("records");
            if (records != null) allRecords.addAll(records);
            Boolean hasMore = (Boolean) page.get("hasMore");
            nextToken = (String) page.get("nextToken");
            if (!Boolean.TRUE.equals(hasMore) || nextToken == null || nextToken.isEmpty()) break;
        } while (true);
        log.info("钉钉 sheet={} 读取完成, 共 {} 条, API次数:{}", sheetId, allRecords.size(), callCounter.get());
        return allRecords;
    }

    public List<Map<String, String>> listRecordsWithFieldNames(String sheetId, String baseId) {
        if (isNearLimit()) {
            log.warn("钉钉API调用次数接近上限({}/{}), 请减少同步频率", callCounter.get(), MONTHLY_LIMIT);
        }
        Map<String, String> fieldMap = fetchFieldMap(baseId, sheetId);
        List<Map<String, Object>> records = listAllRecords(sheetId, baseId);
        List<Map<String, String>> result = new ArrayList<>();
        for (Map<String, Object> rec : records) {
            Map<String, String> row = new LinkedHashMap<>();
            @SuppressWarnings("unchecked")
            Map<String, Object> fields = (Map<String, Object>) rec.get("fields");
            if (fields != null) {
                for (Map.Entry<String, Object> entry : fields.entrySet()) {
                    String chineseName = fieldMap.getOrDefault(entry.getKey(), entry.getKey());
                    row.put(chineseName, extractSimpleValue(entry.getValue()));
                }
            }
            row.put("_recordId", (String) rec.get("id"));
            result.add(row);
        }
        return result;
    }

    // ---- internal ----
    private Map<String, String> fetchFieldMap(String baseId, String sheetId) {
        Map<String, String> map = new LinkedHashMap<>();
        try {
            String token = authService.getAccessToken();
            String url = BASE_URL + "/bases/" + baseId + "/sheets/" + sheetId + "/fields";
            if (props.getOperatorId() != null && !props.getOperatorId().isEmpty())
                url += "?operatorId=" + props.getOperatorId();
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-acs-dingtalk-access-token", token);
            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            callCounter.incrementAndGet();
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> fields = (List<Map<String, Object>>) resp.getBody().get("value");
                if (fields != null) {
                    for (Map<String, Object> f : fields) {
                        String id = (String) f.get("id");
                        String name = (String) f.get("name");
                        if (id != null && name != null) map.put(id, name);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取字段映射失败: {}", e.getMessage());
        }
        return map;
    }

    private Map<String, Object> fetchPage(String baseId, String sheetId, String nextToken) {
        try {
            String token = authService.getAccessToken();
            String url = BASE_URL + "/bases/" + baseId + "/sheets/" + sheetId + "/records/list";
            if (props.getOperatorId() != null && !props.getOperatorId().isEmpty())
                url += "?operatorId=" + props.getOperatorId();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-acs-dingtalk-access-token", token);
            if (props.getOperatorId() != null && !props.getOperatorId().isEmpty())
                headers.set("x-acs-dingtalk-operatorId", props.getOperatorId());
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("maxResults", MAX_RESULTS);
            if (nextToken != null && !nextToken.isEmpty()) body.put("nextToken", nextToken);
            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(body, headers), Map.class);
            callCounter.incrementAndGet();
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) return resp.getBody();
            log.error("钉钉API调用失败: {} {}", resp.getStatusCode(), resp.getBody());
            return null;
        } catch (Exception e) {
            log.error("钉钉API调用异常: {}", e.getMessage());
            return null;
        }
    }

    private String extractSimpleValue(Object val) {
        if (val == null) return "";
        if (val instanceof Map) { Object n = ((Map<?, ?>) val).get("name"); return n != null ? String.valueOf(n) : ""; }
        if (val instanceof List && !((List<?>) val).isEmpty()) {
            Object first = ((List<?>) val).get(0);
            if (first instanceof Map) { Object n = ((Map<?, ?>) first).get("name"); return n != null ? String.valueOf(n) : ""; }
            return String.valueOf(first);
        }
        return String.valueOf(val);
    }

    @Deprecated
    public String getFieldValue(Map<String, Object> record, String fieldName) {
        @SuppressWarnings("unchecked")
        Map<String, Object> fields = (Map<String, Object>) record.get("fields");
        if (fields == null) return null;
        Object val = fields.get(fieldName);
        if (val == null) return null;
        if (val instanceof List && !((List<?>) val).isEmpty()) return String.valueOf(((List<?>) val).get(0));
        return String.valueOf(val);
    }

    @Deprecated
    public String getFieldValue(Map<String, Object> record, String fieldName, String defaultValue) {
        String val = getFieldValue(record, fieldName);
        return val != null ? val : defaultValue;
    }
}
