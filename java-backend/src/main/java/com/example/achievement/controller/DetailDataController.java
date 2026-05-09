package com.example.achievement.controller;

import com.example.achievement.entity.ContractSigning;
import com.example.achievement.entity.RevenueRecognition;
import com.example.achievement.repository.ContractSigningRepository;
import com.example.achievement.repository.RevenueRecognitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/detail")
@RequiredArgsConstructor
public class DetailDataController {

    private final ContractSigningRepository signingRepository;
    private final RevenueRecognitionRepository recognitionRepository;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/signings")
    public ResponseEntity<List<ContractSigning>> getSignings(
            @RequestParam(required = false) String organization,
            @RequestParam(required = false) String signMonth) {
        if (organization != null && signMonth != null) {
            return ResponseEntity.ok(signingRepository.findByOrganizationAndSignMonthStartingWith(organization, signMonth));
        }
        if (signMonth != null) {
            return ResponseEntity.ok(signingRepository.findBySignMonthStartingWith(signMonth));
        }
        if (organization != null) {
            return ResponseEntity.ok(signingRepository.findAll().stream()
                    .filter(s -> organization.equals(s.getOrganization())).collect(Collectors.toList()));
        }
        return ResponseEntity.ok(signingRepository.findAll());
    }

    @PostMapping("/signings")
    public ResponseEntity<ContractSigning> createSigning(@RequestBody ContractSigning signing) {
        signing.setId(UUID.randomUUID().toString());
        return ResponseEntity.ok(signingRepository.save(signing));
    }

    @PutMapping("/signings/{id}")
    public ResponseEntity<ContractSigning> updateSigning(@PathVariable String id, @RequestBody ContractSigning signing) {
        ContractSigning existing = signingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("签约记录不存在: " + id));
        existing.setContractId(signing.getContractId());
        existing.setContractName(signing.getContractName());
        existing.setOrderId(signing.getOrderId());
        existing.setLeadId(signing.getLeadId());
        existing.setPackageId(signing.getPackageId());
        existing.setCustomerName(signing.getCustomerName());
        existing.setAmount(signing.getAmount());
        existing.setAccountingType(signing.getAccountingType());
        existing.setSigningRiskLevel(signing.getSigningRiskLevel());
        existing.setRecognitionRiskLevel(signing.getRecognitionRiskLevel());
        existing.setOperator(signing.getOperator());
        existing.setRemark(signing.getRemark());
        existing.setRegion(signing.getRegion());
        existing.setProductId(signing.getProductId());
        existing.setOrganization(signing.getOrganization());
        existing.setSignMonth(signing.getSignMonth());
        return ResponseEntity.ok(signingRepository.save(existing));
    }

    @DeleteMapping("/signings/{id}")
    public ResponseEntity<Void> deleteSigning(@PathVariable String id) {
        signingRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/signings/migrate-subcategory")
    public ResponseEntity<Map<String, Object>> migrateSigningSubCategory() {
        List<ContractSigning> all = signingRepository.findAll();
        int updated = 0;
        for (ContractSigning s : all) {
            if (s.getSubCategory() == null && s.getSigningRiskLevel() != null) {
                if (s.getSigningRiskLevel().contains("高")) s.setSubCategory("签约收入（高）");
                else if (s.getSigningRiskLevel().contains("中")) s.setSubCategory("签约收入（中）");
                signingRepository.save(s);
                updated++;
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("updated", updated);
        result.put("total", all.size());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/signings/batch")
    public ResponseEntity<Map<String, Object>> batchCreateSignings(@RequestBody List<ContractSigning> signings) {
        int count = 0;
        int skipped = 0;
        for (ContractSigning s : signings) {
            List<ContractSigning> existings = signingRepository.findByContractIdAndPackageId(s.getContractId(), s.getPackageId());
            if (!existings.isEmpty()) { skipped++; continue; }
            s.setId(UUID.randomUUID().toString());
            signingRepository.save(s);
            count++;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        result.put("skipped", skipped);
        result.put("total", signings.size());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/recognitions")
    public ResponseEntity<List<RevenueRecognition>> getRecognitions(
            @RequestParam(required = false) String organization,
            @RequestParam(required = false) String recognitionMonth) {
        if (organization != null && recognitionMonth != null) {
            return ResponseEntity.ok(recognitionRepository.findByOrganizationAndRecognitionMonthStartingWith(organization, recognitionMonth));
        }
        if (recognitionMonth != null) {
            return ResponseEntity.ok(recognitionRepository.findByRecognitionMonthStartingWith(recognitionMonth));
        }
        if (organization != null) {
            return ResponseEntity.ok(recognitionRepository.findAll().stream()
                    .filter(r -> organization.equals(r.getOrganization())).collect(Collectors.toList()));
        }
        return ResponseEntity.ok(recognitionRepository.findAll());
    }

    @PostMapping("/recognitions")
    public ResponseEntity<RevenueRecognition> createRecognition(@RequestBody RevenueRecognition rec) {
        rec.setId(UUID.randomUUID().toString());
        return ResponseEntity.ok(recognitionRepository.save(rec));
    }

    @PutMapping("/recognitions/{id}")
    public ResponseEntity<RevenueRecognition> updateRecognition(@PathVariable String id, @RequestBody RevenueRecognition rec) {
        RevenueRecognition existing = recognitionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("确权记录不存在: " + id));
        existing.setOrderId(rec.getOrderId());
        existing.setContractId(rec.getContractId());
        existing.setContractName(rec.getContractName());
        existing.setRecognitionAmount(rec.getRecognitionAmount());
        existing.setRevenueAmount(rec.getRevenueAmount());
        existing.setRecognitionMonth(rec.getRecognitionMonth());
        existing.setCustomerName(rec.getCustomerName());
        existing.setOperator(rec.getOperator());
        existing.setRemark(rec.getRemark());
        existing.setOrganization(rec.getOrganization());
        existing.setRecognitionRiskLevel(rec.getRecognitionRiskLevel());
        return ResponseEntity.ok(recognitionRepository.save(existing));
    }

    @DeleteMapping("/recognitions/{id}")
    public ResponseEntity<Void> deleteRecognition(@PathVariable String id) {
        recognitionRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/recognitions/batch")
    public ResponseEntity<Map<String, Object>> batchCreateRecognitions(@RequestBody List<RevenueRecognition> recs) {
        int count = 0;
        int skipped = 0;
        for (RevenueRecognition r : recs) {
            List<RevenueRecognition> existings = recognitionRepository.findByContractIdAndOrderId(r.getContractId(), r.getOrderId());
            if (!existings.isEmpty()) { skipped++; continue; }
            r.setId(UUID.randomUUID().toString());
            recognitionRepository.save(r);
            count++;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        result.put("skipped", skipped);
        result.put("total", recs.size());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/recognitions/migrate-subcategory")
    public ResponseEntity<Map<String, Object>> migrateRecognitionSubCategory() {
        List<RevenueRecognition> all = recognitionRepository.findAll();
        int updated = 0;
        for (RevenueRecognition r : all) {
            if (r.getSubCategory() == null) {
                if (r.getRecognitionRiskLevel() != null && r.getRecognitionRiskLevel().contains("高")) {
                    r.setSubCategory("确权收入（高）");
                } else if (r.getRecognitionRiskLevel() != null && r.getRecognitionRiskLevel().contains("中")) {
                    r.setSubCategory("确权收入（中）");
                } else if (r.getRecognitionRiskLevel() == null || r.getRecognitionRiskLevel().isEmpty()) {
                    r.setRecognitionRiskLevel("高");
                    r.setSubCategory("确权收入（高）");
                }
                recognitionRepository.save(r);
                updated++;
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("updated", updated);
        result.put("total", all.size());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/migrate/fix-org-name")
    public ResponseEntity<Map<String, Object>> fixOrgName() {
        String oldName = "AI方案研发机构";
        String newName = "AI方案设计机构";
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("contract_signings", jdbcTemplate.update("UPDATE contract_signings SET organization = ? WHERE organization = ?", newName, oldName));
        counts.put("revenue_recognitions", jdbcTemplate.update("UPDATE revenue_recognitions SET organization = ? WHERE organization = ?", newName, oldName));
        counts.put("targets", jdbcTemplate.update("UPDATE targets SET organization = ? WHERE organization = ?", newName, oldName));
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        Map<String, Object> result = new HashMap<>();
        result.put("oldName", oldName);
        result.put("newName", newName);
        result.put("counts", counts);
        result.put("totalUpdated", total);
        return ResponseEntity.ok(result);
    }
}
