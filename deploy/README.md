# 成果管理系统 — 服务器部署

## 环境要求

- Java 17+
- 无需 Maven（JAR 已打包）
- 无需 Node.js（前端已构建为静态文件）

## 部署步骤

1. 将整个 `deploy/` 目录上传到服务器

2. 配置钉钉（编辑 JAR 内配置或通过环境变量）：
```bash
# 方式一：通过环境变量
export DINGTALK_APPKEY="dingrx0rajaydrsvj9kp"
export DINGTALK_APPSECRET="your_secret"
export DINGTALK_BASEID="7dx2rn0JbY4y54XwI2gz73NMVMGjLRb3"
export DINGTALK_OPERATORID="hrLezKRiP5iSDyzxF9xpIYPwiEiE"
```

3. 启动服务：
```bash
chmod +x start.sh
./start.sh
```

## 目录结构

```
deploy/
├── achievement-service-1.0.0.jar  # Java 后端
├── static/                         # Vue 前端静态文件
├── data/
│   └── achievement.db              # SQLite 数据库
├── logs/                           # 日志目录
└── start.sh                        # 启动脚本
```
