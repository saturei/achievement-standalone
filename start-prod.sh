#!/bin/bash
# ============================================
# 成果管理系统 — 生产环境启动脚本 (JAR 模式)
# 启动：Java JAR 后端 + Vue 前端
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

cleanup() {
    echo ""
    log_info "正在停止所有服务..."
    [ -n "$PID_BACKEND" ]  && kill "$PID_BACKEND"  2>/dev/null
    [ -n "$PID_FRONTEND" ] && kill "$PID_FRONTEND" 2>/dev/null
    log_info "所有服务已停止"
    exit 0
}

trap cleanup SIGINT SIGTERM

# 自动检测部署目录或开发目录
if [ -f "$SCRIPT_DIR/achievement-service-1.0.0.jar" ]; then
    # 部署目录结构 (flat)
    JAR_FILE="$SCRIPT_DIR/achievement-service-1.0.0.jar"
    STATIC_DIR="$SCRIPT_DIR/static"
    USE_STATIC=true
else
    # 开发目录结构
    JAR_FILE="$SCRIPT_DIR/java-backend/target/achievement-service-1.0.0.jar"
    STATIC_DIR=""
    USE_STATIC=false
fi
DATA_DIR="$SCRIPT_DIR/data"
SQLITE_DB="$DATA_DIR/achievement.db"

# ==================== 1. 检查依赖 ====================
log_step "检查运行环境"

if ! command -v java &>/dev/null; then
    log_error "未找到 Java，请安装 Java 17+"
    exit 1
fi
JAVA_VER=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
log_info "Java 版本: $JAVA_VER"

if ! command -v node &>/dev/null; then
    log_error "未找到 Node.js，请安装 Node.js 18+"
    exit 1
fi
log_info "Node.js 版本: $(node -v)"

# ==================== 2. 检查 JAR 文件 ====================
log_step "检查 JAR 文件"

if [ ! -f "$JAR_FILE" ]; then
    log_error "JAR 文件不存在: $JAR_FILE"
    log_error "请先执行构建: cd java-backend && mvn clean package -DskipTests"
    exit 1
fi
log_info "JAR 文件已就绪: $JAR_FILE ($(ls -lh "$JAR_FILE" | awk '{print $5}'))"

# ==================== 3. 检查 SQLite 数据库 ====================
log_step "检查 SQLite 数据库"

if [ ! -d "$DATA_DIR" ]; then
    mkdir -p "$DATA_DIR"
    log_info "创建数据目录: $DATA_DIR"
fi

if [ ! -f "$SQLITE_DB" ]; then
    log_warn "数据库文件不存在: $SQLITE_DB"
    log_warn "请确保 data/achievement.db 已初始化"
else
    SIZE=$(ls -lh "$SQLITE_DB" | awk '{print $5}')
    log_info "数据库已就绪: $SQLITE_DB ($SIZE)"
fi

# ==================== 4. 启动 Java 后端 (JAR 模式) ====================
log_step "启动 Java 后端 (端口 8080, JAR 模式)"

# 使用 sqlite profile，工作目录设为项目根目录
java -jar "$JAR_FILE" \
    --spring.profiles.active=sqlite \
    --spring.datasource.url="jdbc:sqlite:$SQLITE_DB" \
    --server.port=8080 &
PID_BACKEND=$!
log_info "Java 后端 PID: $PID_BACKEND"

# 等待后端就绪
log_info "等待后端启动..."
for i in $(seq 1 30); do
    if curl -s http://localhost:8080/api/detail/dingtalk-status > /dev/null 2>&1; then
        log_info "Java 后端已就绪 (http://localhost:8080)"
        break
    fi
    sleep 2
done

if ! curl -s http://localhost:8080/api/detail/dingtalk-status > /dev/null 2>&1; then
    log_error "Java 后端启动超时，请检查日志"
    cleanup
    exit 1
fi

# ==================== 5. 启动前端 ====================
if [ "$USE_STATIC" = true ]; then
    log_step "启动前端 (静态文件模式, 端口 3000)"
    cd "$STATIC_DIR"
    npx serve -l 3000 --no-clipboard &
    PID_FRONTEND=$!
    log_info "前端 PID: $PID_FRONTEND"
else
    log_step "启动 Vue 前端 (开发模式, 端口 3000)"
    cd "$SCRIPT_DIR/frontend-vue"
    if [ ! -d "node_modules" ] || [ "package.json" -nt "node_modules/.package-lock.json" ]; then
        log_info "正在安装前端依赖..."
        npm install --silent
    fi
    npx vite --host 0.0.0.0 --port 3000 &
    PID_FRONTEND=$!
    log_info "Vue 前端 PID: $PID_FRONTEND"
fi

# 等待前端就绪
for i in $(seq 1 15); do
    if curl -s http://localhost:3000 > /dev/null 2>&1; then
        log_info "Vue 前端已就绪 (http://localhost:3000)"
        break
    fi
    sleep 1
done

# ==================== 6. 总结 ====================
echo ""
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}  成果管理系统已全部启动！（生产模式）${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo -e "  ${BLUE}前端页面:${NC}       http://localhost:3000"
echo -e "  ${BLUE}后端 API:${NC}      http://localhost:8080"
echo -e "  ${BLUE}Swagger 文档:${NC}  http://localhost:8080/swagger-ui.html"
echo -e "  ${BLUE}数据库:${NC}       $SQLITE_DB"
echo ""
echo -e "  ${YELLOW}按 Ctrl+C 停止所有服务${NC}"
echo ""

# 保持脚本运行
wait
