# 成果管理系统启动指南

## 🚀 一键启动（推荐）

```bash
./start.sh
```

脚本会自动完成以下操作：

| 步骤 | 说明 |
|------|------|
| 环境检查 | Java、Maven、Node.js、Python3 |
| 数据库检查 | 验证 `data/achievement.db` SQLite 数据库 |
| 后端构建启动 | Java Spring Boot → 端口 8080 (SQLite) |
| 前端启动 | Vue + Vite → 端口 3000 |
| 钉钉同步 API | FastAPI → 端口 8000 (可选，需 config.yaml，启动失败不阻塞) |
| 统一停止 | Ctrl+C 一键停止所有服务 |

> **注意**: dingtalk-fastapi (端口 8000) 是辅助开发/调试用途的可选组件，生产环境由 Java 后端直连钉钉 API。start.sh 仍会尝试启动它，但启动失败不会导致整体启动中断。

## 📱 服务地址

| 服务 | 地址 |
|------|------|
| 前端页面 | http://localhost:3000 |
| 后端 API | http://localhost:8080 |
| Swagger 文档 | http://localhost:8080/swagger-ui.html |
| 钉钉 API | http://localhost:8000 |
| 钉钉文档 | http://localhost:8000/docs |

## 🗄️ 数据库

项目使用 **SQLite** 数据库，文件位于 `data/achievement.db`。

### 数据导入

当前数据主要通过钉钉同步方式导入，不再依赖 Excel 文件：

```bash
# 通过 Swagger UI 或浏览器调用全量同步
curl -X POST http://localhost:8080/api/data/sync-all
```

同步完成后 Dashboard 和成果管理数据即自动可用。也可以在前端页面中通过「数据同步」按钮触发。

> 历史 Excel 导入脚本 (`reimport_achievements.py` / `init_targets.py`) 仅作参考，当前 targets 数据已由 `DingTalkDataService` 从钉钉数据仓库自动聚合同步。

## 🔐 钉钉登录

### 开发环境 (sqlite/dev profile)
无需认证，所有 API 可直调。兼容旧版 `X-Current-User` header。

### 生产环境 (prod profile)
需通过钉钉 JSAPI 登录：

1. 前端调用 `dd.getAuthCode()` 获取授权码
2. 后端 `POST /api/auth/dingtalk/login` 换取 JWT Token
3. 后续请求携带 `Authorization: Bearer <token>` header

`/api/auth/**` 端点以 `permitAll` 开放，其余 `/api/**` 需 JWT 认证。

## 🔧 钉钉数据同步 API

### 配置

```bash
cd dingtalk-fastapi
cp config.yaml.example config.yaml
# 编辑 config.yaml，填入钉钉应用凭证和表格 ID
```

### 手动启动

```bash
cd dingtalk-fastapi
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

## 📦 部署打包

```bash
# 构建部署包 (JAR + 前端静态文件 + 数据库)
./deploy.sh
```

输出到 `deploy/` 目录：
- `achievement-service-1.0.0.jar` - 后端 JAR
- `static/` - 前端构建产物
- `start.sh` - 部署环境启动脚本

## ⚙️ 手动启动各服务

### Java 后端 (SQLite)

```bash
cd java-backend
mvn clean package -DskipTests
mvn spring-boot:run -Dspring-boot.run.profiles=sqlite
```

### Vue 前端

```bash
cd frontend-vue
npm install
npx vite --host 0.0.0.0 --port 3000
```

## 🔧 常见问题

### 端口被占用

```bash
lsof -i :8080 -i :3000 -i :8000
kill -9 <PID>
```

### 依赖未安装

```bash
# Java 17+
brew install openjdk@17

# Maven
brew install maven

# Node.js
brew install node

# Python3
brew install python3
```

### 数据库不兼容

如果从 H2 切换到 SQLite 后出现 SQL 语法错误，确保 `application-sqlite.yml` 中 `spring.profiles.active` 为 `sqlite`，且不在原生 SQL 中使用双引号包裹列名（SQLite 不认 `"year"` 语法）。
