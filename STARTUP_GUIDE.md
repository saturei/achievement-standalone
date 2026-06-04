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
| 钉钉同步 API | FastAPI → 端口 8000（需 config.yaml） |
| 统一停止 | Ctrl+C 一键停止所有服务 |

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

如果数据库为空，可通过以下方式导入数据：

```bash
# 导入成果数据（成果全量249条.xlsx → achievements 表）
python3 reimport_achievements.py

# 导入目标数据（产品-xl.xlsx → targets 表）
python3 init_targets.py
```

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
