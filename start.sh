#!/bin/bash
# ============================================
# 成果管理系统 — 一键启动脚本
# 启动：Java 后端 + Vue 前端 + 钉钉数据同步 API
# 数据库：SQLite (data/achievement.db)
# ============================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC}  $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_step()  { echo -e "\n${BLUE}==== $1 ====${NC}"; }

PID_BACKEND=""
PID_FRONTEND=""
PID_DINGTALK=""

cleanup() {
    echo ""
    log_info "正在停止所有服务..."
    [ -n "$PID_BACKEND" ]  && kill "$PID_BACKEND"  2>/dev/null
    [ -n "$PID_FRONTEND" ] && kill "$PID_FRONTEND" 2>/dev/null
    [ -n "$PID_DINGTALK" ] && kill "$PID_DINGTALK" 2>/dev/null
    log_info "所有服务已停止"
    exit 0
}

trap cleanup SIGINT SIGTERM

# ==================== 1. 检查依赖 ====================
log_step "检查运行环境"

# Java
if ! command -v java &>/dev/null; then
    log_error "未找到 Java，请安装 Java 17+"
    exit 1
fi
JAVA_VER=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
log_info "Java 版本: $JAVA_VER"

# Maven
if ! command -v mvn &>/dev/null; then
    log_error "未找到 Maven，请安装: brew install maven"
    exit 1
fi
log_info "Maven 已就绪"

# Node
if ! command -v node &>/dev/null; then
    log_error "未找到 Node.js，请安装: brew install node"
    exit 1
fi
log_info "Node.js 版本: $(node -v)"

# Python
if ! command -v python3 &>/dev/null; then
    log_error "未找到 Python3，请安装: brew install python3"
    exit 1
fi
log_info "Python3 版本: $(python3 --version)"

# ==================== 2. 检查 SQLite 数据库 ====================
log_step "检查 SQLite 数据库"

SQLITE_DB="data/achievement.db"
if [ ! -f "$SQLITE_DB" ]; then
    log_warn "数据库文件不存在: $SQLITE_DB"
    log_warn "请确保 data/achievement.db 已初始化，或通过以下方式导入数据："
    log_warn "  python3 reimport_achievements.py    # 导入成果数据"
    log_warn "  python3 init_targets.py             # 导入目标数据"
else
    SIZE=$(ls -lh "$SQLITE_DB" | awk '{print $5}')
    log_info "数据库已就绪: $SQLITE_DB ($SIZE)"
fi

# ==================== 3. 构建并启动 Java 后端 ====================
log_step "启动 Java 后端 (端口 8080, SQLite)"

cd "$SCRIPT_DIR/java-backend"

# 构建 (如果之前构建过且代码未变更则跳过)
if [ ! -f "target/achievement-service-1.0.0.jar" ] || [ "$SCRIPT_DIR/java-backend/src" -nt "target/achievement-service-1.0.0.jar" ]; then
    log_info "正在构建项目..."
    mvn clean package -DskipTests -q
else
    log_info "项目已构建，跳过"
fi

mvn spring-boot:run -Dspring-boot.run.profiles=sqlite -q &
PID_BACKEND=$!
log_info "Java 后端 PID: $PID_BACKEND"

# 等待后端就绪
log_info "等待后端启动..."
for i in $(seq 1 30); do
    if curl -s http://localhost:8080/api/achievements > /dev/null 2>&1; then
        log_info "Java 后端已就绪 (http://localhost:8080)"
        break
    fi
    sleep 2
done

if ! curl -s http://localhost:8080/api/achievements > /dev/null 2>&1; then
    log_error "Java 后端启动超时，请检查日志"
    cleanup
    exit 1
fi

# ==================== 4. 启动 Vue 前端 ====================
log_step "启动 Vue 前端 (端口 3000)"

cd "$SCRIPT_DIR/frontend-vue"

if [ ! -d "node_modules" ] || [ "package.json" -nt "node_modules/.package-lock.json" ]; then
    log_info "正在安装前端依赖..."
    npm install --silent
fi

npx vite --host 0.0.0.0 --port 3000 &
PID_FRONTEND=$!
log_info "Vue 前端 PID: $PID_FRONTEND"

# 等待前端就绪
for i in $(seq 1 15); do
    if curl -s http://localhost:3000 > /dev/null 2>&1; then
        log_info "Vue 前端已就绪 (http://localhost:3000)"
        break
    fi
    sleep 1
done

# ==================== 5. 启动钉钉数据同步 API ====================
log_step "启动钉钉数据同步 API (端口 8000)"

cd "$SCRIPT_DIR/dingtalk-fastapi"

if [ ! -f "config.yaml" ]; then
    log_warn "未找到 dingtalk-fastapi/config.yaml，跳过钉钉服务"
    log_warn "如需启用，请复制 config.yaml.example 并填入钉钉配置"
else
    # 检查并安装 Python 依赖
    if [ ! -d "venv" ]; then
        python3 -m venv venv
    fi
    source venv/bin/activate
    pip install -r requirements.txt -q 2>&1 | tail -1

    uvicorn app.main:app --host 0.0.0.0 --port 8000 &
    PID_DINGTALK=$!
    log_info "钉钉 API PID: $PID_DINGTALK"
    log_info "钉钉 API 已就绪 (http://localhost:8000)"
    log_info "API 文档: http://localhost:8000/docs"
fi

# ==================== 6. 总结 ====================
echo ""
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}  成果管理系统已全部启动！${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo -e "  ${BLUE}前端页面:${NC}       http://localhost:3000"
echo -e "  ${BLUE}后端 API:${NC}      http://localhost:8080"
echo -e "  ${BLUE}Swagger 文档:${NC}  http://localhost:8080/swagger-ui.html"
if [ -n "$PID_DINGTALK" ]; then
    echo -e "  ${BLUE}钉钉 API:${NC}      http://localhost:8000"
    echo -e "  ${BLUE}钉钉文档:${NC}     http://localhost:8000/docs"
fi
echo -e "  ${BLUE}数据库:${NC}       $SCRIPT_DIR/$SQLITE_DB"
echo ""
echo -e "  ${YELLOW}按 Ctrl+C 停止所有服务${NC}"
echo ""

# 保持脚本运行
wait
