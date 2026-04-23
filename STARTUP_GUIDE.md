# 成果管理系统启动指南

## 🚀 快速启动

### 方式一：使用启动脚本（推荐）

```bash
./start.sh
```

启动脚本会自动：
1. 检查Java版本（需要Java 17+）
2. 安装必要的工具（Maven、Java 17）
3. 构建项目
4. 启动应用（使用H2内存数据库）

### 方式二：手动启动

#### 1. 安装Java 17

**使用SDKMAN（推荐）：**
```bash
# 安装SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# 安装Java 17
sdk install java 17.0.9-tem
sdk use java 17.0.9-tem
```

**使用Homebrew：**
```bash
brew install openjdk@17
sudo ln -sfn /usr/local/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk
```

#### 2. 安装Maven

**使用SDKMAN：**
```bash
sdk install maven
```

**使用Homebrew：**
```bash
brew install maven
```

#### 3. 启动应用

```bash
cd java-backend
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

### 方式三：使用Docker

#### 1. 启动Docker Desktop

确保Docker Desktop应用已启动。

#### 2. 构建并运行

```bash
# 构建镜像
cd java-backend
docker build -t achievement-service .

# 运行容器
docker run -p 8080:8080 achievement-service
```

## 📱 访问应用

启动成功后，可以访问：

- **应用地址**: http://localhost:8080
- **API文档**: http://localhost:8080/swagger-ui.html
- **H2控制台**: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:achievement_db`
  - 用户名: `sa`
  - 密码: （留空）

## 🧪 测试API

### 获取成果列表
```bash
curl http://localhost:8080/api/achievements
```

### 获取成果统计
```bash
curl http://localhost:8080/api/achievements/statistics
```

### 创建成果预注册
```bash
curl -X POST http://localhost:8080/api/achievements/pre-register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "测试成果",
    "productId": "P001",
    "type": "模块",
    "owner": "张三",
    "description": "这是一个测试成果"
  }'
```

## ⚙️ 配置说明

### 使用H2内存数据库（默认）
- 配置文件: `application-h2.yml`
- 数据会在应用重启后清空
- 适合快速测试和演示

### 使用MySQL数据库
1. 启动MySQL服务
2. 执行数据库初始化脚本：
   ```bash
   mysql -u root -p < database/init_database.sql
   ```
3. 修改配置文件 `application.yml`：
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/achievement_db
       username: root
       password: your_password
   ```
4. 启动应用：
   ```bash
   mvn spring-boot:run
   ```

## 🔧 常见问题

### 1. Java版本不匹配
**错误**: `Unsupported class file major version 61`

**解决**: 安装Java 17或更高版本

### 2. Maven命令未找到
**错误**: `command not found: mvn`

**解决**: 安装Maven（见上文）

### 3. 端口被占用
**错误**: `Port 8080 already in use`

**解决**: 
```bash
# 查找占用端口的进程
lsof -i :8080

# 终止进程
kill -9 <PID>

# 或修改端口
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

### 4. Docker守护进程未运行
**错误**: `failed to connect to the docker API`

**解决**: 启动Docker Desktop应用

## 📚 更多信息

- [项目README](java-backend/README.md)
- [API文档](docs/API.md)
- [数据库设计](docs/DATABASE.md)
