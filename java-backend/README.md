# 成果管理系统 Java 后端

基于 Spring Boot 3.x 的成果管理系统后端服务。

## 技术栈

- **框架**: Spring Boot 3.2.0
- **ORM**: Spring Data JPA
- **数据库**: MySQL 8.0+
- **认证**: Spring Security
- **API文档**: SpringDoc OpenAPI (Swagger)
- **数据库迁移**: Flyway
- **构建工具**: Maven

## 项目结构

```
java-backend/
├── src/main/java/com/example/achievement/
│   ├── AchievementApplication.java          # 启动类
│   ├── config/                               # 配置类
│   │   ├── SecurityConfig.java
│   │   └── SwaggerConfig.java
│   ├── controller/                           # 控制器层
│   │   └── AchievementController.java
│   ├── service/                              # 服务层
│   │   ├── AchievementService.java
│   │   └── impl/AchievementServiceImpl.java
│   ├── repository/                           # 数据访问层
│   │   ├── AchievementRepository.java
│   │   ├── AchievementStatusRecordRepository.java
│   │   └── AchievementVersionRecordRepository.java
│   ├── entity/                               # 实体类
│   │   ├── Achievement.java
│   │   ├── AchievementStatusRecord.java
│   │   └── AchievementVersionRecord.java
│   ├── dto/                                  # 数据传输对象
│   │   ├── request/
│   │   └── response/
│   ├── enums/                                # 枚举类
│   │   ├── AchievementStatus.java
│   │   └── ChangeType.java
│   ├── exception/                            # 异常处理
│   │   ├── GlobalExceptionHandler.java
│   │   ├── AchievementNotFoundException.java
│   │   └── InvalidStatusTransitionException.java
│   └── util/                                 # 工具类
│       └── VersionGenerator.java
├── src/main/resources/
│   ├── application.yml                       # 主配置文件
│   ├── application-dev.yml                   # 开发环境配置
│   ├── application-prod.yml                  # 生产环境配置
│   └── db/migration/                         # 数据库迁移脚本
│       ├── V1__init_schema.sql
│       └── V2__add_sample_data.sql
└── pom.xml
```

## 快速开始

### 前置要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+

### 数据库准备

1. 创建数据库：

```sql
CREATE DATABASE achievement_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 修改配置文件：

编辑 `src/main/resources/application.yml`，修改数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/achievement_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: your_username
    password: your_password
```

### 运行项目

1. 编译项目：

```bash
cd java-backend
mvn clean install
```

2. 运行应用：

```bash
mvn spring-boot:run
```

或者指定环境：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

3. 访问应用：

- 应用地址: http://localhost:8080
- API文档: http://localhost:8080/swagger-ui.html

## API 接口

### 成果管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/achievements | 获取成果列表 |
| GET | /api/achievements/statistics | 获取成果统计 |
| GET | /api/achievements/{id} | 获取成果详情 |
| POST | /api/achievements/pre-register | 成果预注册 |
| POST | /api/achievements/{id}/register | 成果注册 |
| POST | /api/achievements/{id}/record | 成果登记 |
| POST | /api/achievements/{id}/change | 成果变更 |
| PUT | /api/achievements/{id}/offline | 成果下架 |
| PUT | /api/achievements/{id}/online | 成果上架 |
| PUT | /api/achievements/{id}/delete | 成果删除 |

详细API文档请访问 Swagger UI: http://localhost:8080/swagger-ui.html

## 成果状态流转

```
预注册 (pre_register)
    ├── 注册 (register)
    │       └── 登记 (record)
    │               ├── 下架 (offline)
    │               │       └── 上架 (record)
    │               └── ...
    └── 删除 (deleted)
```

## 配置说明

### 数据库配置

- `spring.datasource.url`: 数据库连接URL
- `spring.datasource.username`: 数据库用户名
- `spring.datasource.password`: 数据库密码

### JPA配置

- `spring.jpa.hibernate.ddl-auto`: 设置为 `validate` 以验证实体与数据库表的一致性
- `spring.jpa.show-sql`: 是否显示SQL语句

### Flyway配置

- `spring.flyway.enabled`: 是否启用Flyway
- `spring.flyway.locations`: 迁移脚本位置
- `spring.flyway.baseline-on-migrate`: 是否在迁移时创建基线

## 开发指南

### 添加新的API接口

1. 在 `controller` 包中创建或修改控制器
2. 在 `service` 包中定义服务接口和实现
3. 在 `repository` 包中添加数据访问方法
4. 如需新的实体，在 `entity` 包中创建
5. 更新 Swagger 注解以生成API文档

### 数据库迁移

1. 在 `src/main/resources/db/migration/` 目录下创建新的迁移脚本
2. 文件命名格式: `V{version}__{description}.sql`
3. 重启应用，Flyway会自动执行迁移

## 测试

运行测试：

```bash
mvn test
```

## 打包部署

打包为JAR文件：

```bash
mvn clean package
```

运行JAR文件：

```bash
java -jar target/achievement-service-1.0.0.jar
```

## 注意事项

1. **ID生成**: 成果ID使用 `成果名称_版本号` 格式
2. **状态流转**: 严格按照状态机进行状态变更
3. **版本管理**: 每次变更生成新版本并记录变更历史
4. **数据校验**: 使用 Spring Validation 进行参数校验
5. **异常处理**: 统一使用 GlobalExceptionHandler 处理异常

## 许可证

MIT License
