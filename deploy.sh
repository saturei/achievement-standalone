#!/bin/bash
# ============================================
# 成果管理系统 — 部署打包脚本
# 构建 JAR + 前端 + 数据库 → deploy/ 目录
# ============================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC}  $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

DEPLOY_DIR="$SCRIPT_DIR/deploy"
JAR_FILE="$SCRIPT_DIR/java-backend/target/achievement-service-1.0.0.jar"

log_info "开始构建部署包..."

# 清理旧的部署目录
rm -rf "$DEPLOY_DIR"
mkdir -p "$DEPLOY_DIR"/{data,logs}

# ==================== 1. 构建后端 JAR ====================
echo ""
echo -e "${BLUE}==== 构建 Java 后端 JAR ====${NC}"

cd "$SCRIPT_DIR/java-backend"
mvn clean package -DskipTests -q 2>&1 | tail -3
cp "$JAR_FILE" "$DEPLOY_DIR/"
log_info "JAR 文件: $(ls -lh "$DEPLOY_DIR/$(basename $JAR_FILE)" | awk '{print $5}')"

# ==================== 2. 构建前端 ====================
echo ""
echo -e "${BLUE}==== 构建 Vue 前端 ====${NC}"

cd "$SCRIPT_DIR/frontend-vue"
npm run build -- --outDir "$DEPLOY_DIR/static" 2>&1 | tail -2
log_info "前端静态文件: $DEPLOY_DIR/static/"

# ==================== 3. 复制数据库 ====================
echo ""
echo -e "${BLUE}==== 复制数据库 ====${NC}"

if [ -f "$SCRIPT_DIR/data/achievement.db" ]; then
    cp "$SCRIPT_DIR/data/achievement.db" "$DEPLOY_DIR/data/"
    log_info "数据库: $(ls -lh "$DEPLOY_DIR/data/achievement.db" | awk '{print $5}')"
else
    log_info "数据库文件不存在，跳过（首次部署需从钉钉同步）"
fi

# ==================== 4. 复制启动脚本 ====================
cp "$SCRIPT_DIR/start-prod.sh" "$DEPLOY_DIR/start.sh"
chmod +x "$DEPLOY_DIR/start.sh"

# ==================== 5. 生成 Server 部署说明 ====================
cat > "$DEPLOY_DIR/README.md" << 'README'
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
README

echo ""
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}  部署包构建完成！${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo -e "  输出目录: ${BLUE}$DEPLOY_DIR${NC}"
echo -e "  文件大小: $(du -sh "$DEPLOY_DIR" | awk '{print $1}')"
echo ""
echo -e "  上传到服务器: ${BLUE}scp -r deploy/ user@server:~/achievement/${NC}"
echo -e "  服务器启动:   ${BLUE}cd ~/achievement && ./start.sh${NC}"
echo ""
