# 成果管理模块 Java 实现指南

## 技术栈推荐

### 后端框架（实际使用）
- **Spring Boot 2.7.18** - 主框架
- **Spring Data JPA** + **JDBC Template** - ORM + 原生SQL
- **Spring Security** - 安全框架（开发模式放行 /api/**，权限通过 X-Current-User 头控制）
- **Lombok** - 简化代码

### 数据库（实际使用）
- **SQLite** (本地开发) / **H2** (测试) / **MySQL 8.0+** (生产)
- DDL: spring.jpa.hibernate.ddl-auto = update (SQLite) / validate (MySQL)

### 其他
- **Maven** 或 **Gradle** - 项目构建
- **Swagger/OpenAPI** - API文档
- **JUnit 5 + Mockito** - 单元测试

---

## 项目结构建议

```
achievement-service/
├── src/main/java/com/example/achievement/
│   ├── AchievementApplication.java          # 启动类
│   │
│   ├── config/                               # 配置类
│   │   ├── SecurityConfig.java
│   │   ├── SwaggerConfig.java
│   │   └── JpaConfig.java
│   │
│   ├── controller/                           # 控制器层
│   │   ├── AchievementController.java
│   │   └── AchievementVersionRecordController.java
│   │
│   ├── service/                              # 服务层
│   │   ├── AchievementService.java
│   │   ├── AchievementServiceImpl.java
│   │   ├── AchievementVersionService.java
│   │   └── AchievementVersionServiceImpl.java
│   │
│   ├── repository/                           # 数据访问层
│   │   ├── AchievementRepository.java
│   │   ├── AchievementStatusRecordRepository.java
│   │   └── AchievementVersionRecordRepository.java
│   │
│   ├── entity/                               # 实体类
│   │   ├── Achievement.java
│   │   ├── AchievementStatusRecord.java
│   │   └── AchievementVersionRecord.java
│   │
│   ├── dto/                                  # 数据传输对象
│   │   ├── request/
│   │   │   ├── AchievementPreRegisterRequest.java
│   │   │   ├── AchievementChangeRequest.java
│   │   │   ├── AchievementOfflineRequest.java
│   │   │   └── AchievementOnlineRequest.java
│   │   └── response/
│   │       ├── AchievementResponse.java
│   │       ├── AchievementListResponse.java
│   │       └── AchievementStatisticsResponse.java
│   │
│   ├── enums/                                # 枚举类
│   │   ├── AchievementStatus.java
│   │   ├── AchievementType.java
│   │   └── ChangeType.java
│   │
│   ├── exception/                            # 异常处理
│   │   ├── GlobalExceptionHandler.java
│   │   ├── AchievementNotFoundException.java
│   │   └── InvalidStatusTransitionException.java
│   │
│   └── util/                                 # 工具类
│       ├── VersionGenerator.java
│       └── DateTimeUtil.java
│
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   └── db/migration/                         # Flyway迁移脚本
│       ├── V1__init_schema.sql
│       └── V2__add_sample_data.sql
│
└── pom.xml
```

---

## 实体类设计

### Achievement 实体类

```java
@Entity
@Table(name = "achievements")
@Data
public class Achievement {
    
    @Id
    @Column(length = 50)
    private String id;
    
    // 基础数据
    @Column(name = "product_id", nullable = false, length = 50)
    private String productId;
    
    @Column(name = "product_name", length = 200)
    private String productName;
    
    @Column(name = "organization_id", length = 50)
    private String organizationId;
    
    @Column(name = "organization_name", length = 200)
    private String organizationName;
    
    @Column(name = "department_id", length = 50)
    private String departmentId;
    
    @Column(name = "department_name", length = 200)
    private String departmentName;
    
    @Column(nullable = false, length = 200)
    private String name;
    
    @Column(length = 50)
    private String version;
    
    @Column(name = "product_external_version", length = 50)
    private String productExternalVersion;
    
    @Column(name = "has_baseline", length = 20)
    private String hasBaseline = "无基线";
    
    @Column(name = "requirement_proposer", length = 100)
    private String requirementProposer;
    
    @Column(name = "achievement_form", length = 100)
    private String achievementForm;
    
    @Column(name = "sale_type", length = 100)
    private String saleType;
    
    @Column(name = "function_list_file", length = 500)
    private String functionListFile;
    
    @Column(name = "package_ids", columnDefinition = "JSON")
    @Convert(converter = JsonListConverter.class)
    private List<String> packageIds;
    
    @Column(name = "application_scenario", columnDefinition = "TEXT")
    private String applicationScenario;
    
    @Column(name = "module_id", length = 50)
    private String moduleId;
    
    @Column(name = "module_name", length = 200)
    private String moduleName;
    
    @Column(length = 50)
    private String type;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(length = 100)
    private String owner;
    
    // 计划数据
    @Column(name = "achievement_target", columnDefinition = "TEXT")
    private String achievementTarget;
    
    @Column(name = "planned_acceptance_date")
    private LocalDate plannedAcceptanceDate;
    
    @Column(name = "acceptance_method", columnDefinition = "TEXT")
    private String acceptanceMethod;
    
    @Column(length = 200)
    private String acceptor;
    
    @Column(name = "related_project_id", length = 50)
    private String relatedProjectId;
    
    @Column(name = "related_project_name", length = 200)
    private String relatedProjectName;
    
    @Column(name = "related_order_id", length = 50)
    private String relatedOrderId;
    
    @Column(name = "related_order_name", length = 200)
    private String relatedOrderName;
    
    @Column(name = "acceptance_requirements", columnDefinition = "TEXT")
    private String acceptanceRequirements;
    
    @Column(name = "acceptance_organization", length = 200)
    private String acceptanceOrganization;
    
    // 变更数据
    @Column(name = "change_reason", columnDefinition = "TEXT")
    private String changeReason;
    
    // 实际数据
    @Column(columnDefinition = "TEXT")
    private String deliverables;
    
    @Column(name = "code_repository_url", length = 500)
    private String codeRepositoryUrl;
    
    @Column(name = "demo_url", length = 500)
    private String demoUrl;
    
    @Column(name = "actual_acceptance_date")
    private LocalDate actualAcceptanceDate;
    
    // 系统审计数据
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private AchievementStatus status = AchievementStatus.PRE_REGISTER;
    
    @Column(name = "estimated_acceptance_month", length = 20)
    private String estimatedAcceptanceMonth;
    
    @Column(name = "pre_register_time")
    private LocalDateTime preRegisterTime;
    
    @Column(name = "register_time")
    private LocalDateTime registerTime;
    
    @Column(name = "record_time")
    private LocalDateTime recordTime;
    
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // 其他
    @Column(name = "risk_tags", columnDefinition = "JSON")
    @Convert(converter = JsonListConverter.class)
    private List<String> riskTags;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (preRegisterTime == null) {
            preRegisterTime = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### AchievementStatus 枚举类

```java
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
}
```

---

## API接口映射

### Controller 示例

```java
@RestController
@RequestMapping("/api/achievements")
@Tag(name = "成果管理", description = "成果管理相关接口")
public class AchievementController {
    
    @Autowired
    private AchievementService achievementService;
    
    @GetMapping
    @Operation(summary = "获取成果列表")
    public ResponseEntity<AchievementListResponse> getAchievements(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String departmentName,
            @RequestParam(required = false) String organizationNames,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) Boolean includeDeleted) {
        
        AchievementListResponse response = achievementService.getAchievements(
            page, pageSize, keyword, departmentName, organizationNames, status, productId, includeDeleted);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/filter-options")
    @Operation(summary = "获取级联筛选选项（部门和机构）")
    public ResponseEntity<Map<String, List<String>>> getFilteredOptions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String departmentName,
            @RequestParam(required = false) String organizationNames,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String productId) {
        return ResponseEntity.ok(achievementService.getFilteredOptions(
                keyword, departmentName, organizationNames, status, productId));
    }

    @GetMapping("/departments")
    @Operation(summary = "获取所有部门列表")
    public ResponseEntity<List<String>> getAllDepartments() {
        return ResponseEntity.ok(achievementService.getAllDepartments());
    }

    @GetMapping("/organizations")
    @Operation(summary = "获取所有机构列表")
    public ResponseEntity<List<String>> getAllOrganizations() {
        return ResponseEntity.ok(achievementService.getAllOrganizations());
    }

    @GetMapping("/owners")
    @Operation(summary = "获取所有负责人列表")
    public ResponseEntity<List<String>> getAllOwners() {
        return ResponseEntity.ok(achievementService.getAllOwners());
    }
    
    @GetMapping("/statistics")
    @Operation(summary = "获取成果统计")
    public ResponseEntity<AchievementStatisticsResponse> getStatistics() {
        return ResponseEntity.ok(achievementService.getStatistics());
    }
    
    @GetMapping("/{achievementId}")
    @Operation(summary = "获取成果详情")
    public ResponseEntity<AchievementResponse> getAchievement(
            @PathVariable String achievementId) {
        return ResponseEntity.ok(achievementService.getAchievement(achievementId));
    }
    
    @PostMapping("/pre-register")
    @Operation(summary = "成果预注册")
    public ResponseEntity<AchievementResponse> preRegister(
            @Valid @RequestBody AchievementPreRegisterRequest request) {
        return ResponseEntity.ok(achievementService.preRegister(request));
    }
    
    @PostMapping("/{achievementId}/register")
    @Operation(summary = "成果注册")
    public ResponseEntity<AchievementResponse> register(
            @PathVariable String achievementId,
            @RequestBody AchievementRegisterRequest request) {
        return ResponseEntity.ok(achievementService.register(achievementId, request));
    }
    
    @PostMapping("/{achievementId}/record")
    @Operation(summary = "成果登记")
    public ResponseEntity<AchievementResponse> record(
            @PathVariable String achievementId,
            @RequestBody AchievementRecordRequest request) {
        return ResponseEntity.ok(achievementService.record(achievementId, request));
    }
    
    @PostMapping("/{achievementId}/change")
    @Operation(summary = "成果变更")
    public ResponseEntity<AchievementResponse> change(
            @PathVariable String achievementId,
            @Valid @RequestBody AchievementChangeRequest request) {
        return ResponseEntity.ok(achievementService.change(achievementId, request));
    }
    
    @PutMapping("/{achievementId}/offline")
    @Operation(summary = "成果下架")
    public ResponseEntity<AchievementResponse> offline(
            @PathVariable String achievementId,
            @RequestBody AchievementOfflineRequest request) {
        return ResponseEntity.ok(achievementService.offline(achievementId, request));
    }
    
    @PutMapping("/{achievementId}/online")
    @Operation(summary = "成果上架")
    public ResponseEntity<AchievementResponse> online(
            @PathVariable String achievementId,
            @RequestBody AchievementOnlineRequest request) {
        return ResponseEntity.ok(achievementService.online(achievementId, request));
    }
    
    @PutMapping("/{achievementId}/delete")
    @Operation(summary = "成果删除")
    public ResponseEntity<AchievementResponse> delete(
            @PathVariable String achievementId) {
        return ResponseEntity.ok(achievementService.delete(achievementId));
    }
}
```

---

## 版本号生成策略

```java
@Component
public class VersionGenerator {
    
    @Autowired
    private AchievementRepository achievementRepository;
    
    /**
     * 生成管理版本号
     * 格式: V{主版本}.{次版本}.{修订号}
     */
    public String generateManagementVersion(String achievementName, String productExternalVersion) {
        // 查询同名成果的最新版本
        Optional<Achievement> latestAchievement = achievementRepository
            .findTopByNameAndProductExternalVersionOrderByCreatedAtDesc(
                achievementName, productExternalVersion);
        
        if (latestAchievement.isEmpty()) {
            return "V1.0.0";
        }
        
        String currentVersion = latestAchievement.get().getVersion();
        return incrementVersion(currentVersion);
    }
    
    private String incrementVersion(String version) {
        // 移除V前缀
        String versionNum = version.replace("V", "");
        String[] parts = versionNum.split("\\.");
        
        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        int patch = Integer.parseInt(parts[2]);
        
        // 递增修订号
        patch++;
        if (patch > 99) {
            patch = 0;
            minor++;
            if (minor > 99) {
                minor = 0;
                major++;
            }
        }
        
        return String.format("V%d.%d.%d", major, minor, patch);
    }
}
```

---

## 配置文件示例

### application.yml

```yaml
spring:
  application:
    name: achievement-service
  
  datasource:
    url: jdbc:mysql://localhost:3306/achievement_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQL8Dialect
  
  flyway:
    enabled: true
    locations: classpath:db/migration

server:
  address: 0.0.0.0
  port: 8080

jwt:
  secret: your-secret-key
  expiration: 86400000

logging:
  level:
    com.example.achievement: DEBUG
```

---

## 注意事项

1. **ID生成**: 成果ID使用 `成果名称_版本号` 格式，需要确保唯一性
2. **状态流转**: 严格按照状态机进行状态变更，不允许跳跃
3. **版本管理**: 每次变更需要生成新版本并记录变更历史
4. **并发控制**: 使用乐观锁或悲观锁防止并发修改问题
5. **数据校验**: 使用Spring Validation进行参数校验
6. **异常处理**: 统一使用GlobalExceptionHandler处理异常
7. **日志记录**: 记录关键操作的日志，便于问题追踪
8. **内网访问**: `server.address: 0.0.0.0` 确保服务绑定IPv4，允许内网其他设备访问
9. **SQLite模式**: 开发时使用 `-Dspring-boot.run.profiles=sqlite` 激活SQLite数据库，主配置默认使用MySQL
10. **级联筛选**: `/filter-options` 接口根据当前已选条件返回可用选项，实现部门和机构下拉联动缩小范围
11. **目标统计实值来源**: 签约实际值优先从 `contract_signings.amount` 聚合，确权实际值优先从 `revenue_recognitions.revenue_amount`（含税收入）聚合，无数据回退到 targets.q1~q4_actual
12. **细分目标推导**: 签约/确权明细保存时根据风险等级自动填充 subCategory（高→（高）、中→（中））
13. **明细去重**: 签约导入按 contractId+packageId 去重，确权导入按 contractId+orderId 去重
14. **金额单位**: 页面显示万元，数据库存元，保存时前端 ×10000 入库、加载时 ÷10000 展示
15. **月份兼容**: 粘贴 Excel 时支持 `2026年3月` / `2026-03` / `2026-3` 多种格式的月份
16. **权限控制**: 通过 `X-Current-User` 请求头传递用户名，`UserContextFilter` 提取到 ThreadLocal，Service 层按角色过滤

---

## v2.0 新增实体（明细数据+用户）

### 签约明细 (ContractSigning)
```java
@Entity @Table(name = "contract_signings") @Data
public class ContractSigning {
    private String contractId, contractName, orderId, leadId, packageId;
    private String customerName;
    private BigDecimal amount;               // 签约金额(元)
    private String signingRiskLevel;          // 高/中/低 → 驱动 subCategory
    private String subCategory;               // 自动计算：签约收入（高）/签约收入（中）
    private String signMonth;                 // YYYY-MM
    private String organization;
    // ... 其他字段

    @PrePersist @PreUpdate
    private void calcYearQuarter() { ... }    // signMonth → year + quarter
    @PrePersist @PreUpdate
    private void calcSubCategory() { ... }    // signingRiskLevel → subCategory
}
```

### 确权明细 (RevenueRecognition)
```java
@Entity @Table(name = "revenue_recognitions") @Data
public class RevenueRecognition {
    private String orderId, contractId, contractName;
    private BigDecimal recognitionAmount;     // 确权金额(元，税前)
    private BigDecimal revenueAmount;         // 收入金额(元，含税)
    private String recognitionRiskLevel;      // 高/中/低 → 驱动 subCategory
    private String subCategory;               // 自动计算：确权收入（高）/确权收入（中）
    private String recognitionMonth;          // YYYY-MM
    private String organization;
}
```

### 用户配置 (UserConfig)
```java
@Entity @Table(name = "user_configs") @Data
public class UserConfig {
    private String username;                  // 唯一
    private String displayName;
    private String role;                      // ADMIN/DEPT_LEADER/ORG_LEADER/USER
    private String department;
    private String organization;
    private Integer enabled;                  // 1/0
}
```

### TargetServiceImpl 统计联动关键代码
```java
// 预加载明细聚合数据
List<Object[]> signingSums = signingRepository.sumAmountByOrgYearQuarter();
for (Object[] row : signingSums) {
    String key = row[0] + "_" + row[1] + "_" + row[2] + "_" + row[3]; // org_year_q_subCat
    signingMap.put(key, new BigDecimal(row[4].toString())); // amount
}
// 在遍历 targets 时，按 organization_year_quarter_subCategory 匹配明细聚合值
String key = organizationName + "_" + year + "_" + q + "_" + subCategoryName;
BigDecimal val = signingMap.get(key);
// 有明细值用明细，无则回退到 targets 表中的手工值
```
