package com.example.achievement.service;

import com.example.achievement.config.DingTalkProperties;
import com.example.achievement.entity.ContractSigning;
import com.example.achievement.entity.RevenueRecognition;
import com.example.achievement.repository.ContractSigningRepository;
import com.example.achievement.repository.RevenueRecognitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class DingTalkSyncService {

    private final DingTalkClient dingTalkClient;
    private final DingTalkProperties props;
    private final ContractSigningRepository signingRepository;
    private final RevenueRecognitionRepository recognitionRepository;

    private static final Pattern MONTH_CN = Pattern.compile("(\\d{4})\\s*年\\s*(\\d{1,2})\\s*月");
    private static final Pattern MONTH_DASH = Pattern.compile("^(\\d{4})-(\\d{1,2})$");

    public Map<String, Object> syncSignings() {
        List<Map<String, Object>> records = dingTalkClient.listAllRecords(props.getSheetSignings());
        int imported = 0;
        int skipped = 0;

        for (Map<String, Object> raw : records) {
            try {
                ContractSigning s = mapToSigning(raw);
                List<ContractSigning> existings = signingRepository.findByContractIdAndPackageId(
                        s.getContractId(), s.getPackageId());
                if (!existings.isEmpty()) {
                    skipped++;
                    continue;
                }
                s.setId(UUID.randomUUID().toString());
                signingRepository.save(s);
                imported++;
            } catch (Exception e) {
                log.warn("签约明细导入跳过一条: {}", e.getMessage());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imported", imported);
        result.put("skipped", skipped);
        result.put("total", records.size());
        return result;
    }

    public Map<String, Object> syncRecognitions() {
        List<Map<String, Object>> records = dingTalkClient.listAllRecords(props.getSheetRecognitions());
        int imported = 0;
        int skipped = 0;

        for (Map<String, Object> raw : records) {
            try {
                RevenueRecognition r = mapToRecognition(raw);
                List<RevenueRecognition> existings = recognitionRepository.findByContractIdAndOrderId(
                        r.getContractId(), r.getOrderId());
                if (!existings.isEmpty()) {
                    skipped++;
                    continue;
                }
                r.setId(UUID.randomUUID().toString());
                recognitionRepository.save(r);
                imported++;
            } catch (Exception e) {
                log.warn("确权明细导入跳过一条: {}", e.getMessage());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imported", imported);
        result.put("skipped", skipped);
        result.put("total", records.size());
        return result;
    }

    private ContractSigning mapToSigning(Map<String, Object> record) {
        ContractSigning s = new ContractSigning();
        s.setContractId(dingTalkClient.getFieldValue(record, "合同ID", ""));
        s.setContractName(dingTalkClient.getFieldValue(record, "合同名称", ""));
        s.setOrderId(dingTalkClient.getFieldValue(record, "订单ID", ""));
        s.setLeadId(dingTalkClient.getFieldValue(record, "关联线索ID", ""));
        s.setPackageId(dingTalkClient.getFieldValue(record, "产品套餐ID", ""));
        s.setCustomerName(dingTalkClient.getFieldValue(record, "关联客户", ""));
        s.setAmount(parseWanToYuan(dingTalkClient.getFieldValue(record, "签约金额(万元)", "0")));
        s.setAccountingType(dingTalkClient.getFieldValue(record, "合同核算类型", ""));
        s.setOrganization(dingTalkClient.getFieldValue(record, "所属机构", ""));
        s.setSignMonth(parseMonth(dingTalkClient.getFieldValue(record, "签约归属年月", "")));
        s.setSigningRiskLevel(dingTalkClient.getFieldValue(record, "签约风险等级", ""));
        s.setRecognitionRiskLevel(dingTalkClient.getFieldValue(record, "确权风险等级", ""));
        s.setOperator(dingTalkClient.getFieldValue(record, "经营岗", ""));
        s.setRemark(dingTalkClient.getFieldValue(record, "备注", ""));
        s.setRegion(dingTalkClient.getFieldValue(record, "所属分区", ""));
        s.setProductId(dingTalkClient.getFieldValue(record, "产品ID", ""));
        return s;
    }

    private RevenueRecognition mapToRecognition(Map<String, Object> record) {
        RevenueRecognition r = new RevenueRecognition();
        r.setOrderId(dingTalkClient.getFieldValue(record, "订单ID", ""));
        r.setContractId(dingTalkClient.getFieldValue(record, "合同ID", ""));
        r.setContractName(dingTalkClient.getFieldValue(record, "合同名称", ""));
        r.setRecognitionAmount(parseWanToYuan(dingTalkClient.getFieldValue(record, "确权金额(万元)", "0")));
        r.setRevenueAmount(parseWanToYuan(dingTalkClient.getFieldValue(record, "收入金额(万元,含税)", "0")));
        r.setRecognitionMonth(parseMonth(dingTalkClient.getFieldValue(record, "确权归属年月", "")));
        r.setRecognitionRiskLevel(dingTalkClient.getFieldValue(record, "确权风险等级", ""));
        r.setCustomerName(dingTalkClient.getFieldValue(record, "关联客户", ""));
        r.setOperator(dingTalkClient.getFieldValue(record, "经营岗", ""));
        r.setRemark(dingTalkClient.getFieldValue(record, "备注", ""));
        r.setOrganization(dingTalkClient.getFieldValue(record, "所属机构", ""));
        return r;
    }

    private BigDecimal parseWanToYuan(String wan) {
        try {
            double d = Double.parseDouble(wan.trim());
            return BigDecimal.valueOf(Math.round(d * 10000));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private String parseMonth(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "";
        String m = raw.trim();
        Matcher cn = MONTH_CN.matcher(m);
        if (cn.find()) return cn.group(1) + "-" + cn.group(2).replaceFirst("^0+", "");
        Matcher dash = MONTH_DASH.matcher(m);
        if (dash.matches()) return dash.group(1) + "-" + dash.group(2).replaceFirst("^0+", "");
        return m;
    }
}
