#!/bin/bash

echo "========================================="
echo "成果管理系统启动脚本"
echo "========================================="

# 检查Java版本
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
echo "当前Java版本: $JAVA_VERSION"

# 如果Java版本低于17，使用SDKMAN安装Java 17
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "需要Java 17或更高版本，正在安装..."
    
    # 安装SDKMAN
    if [ ! -d "$HOME/.sdkman" ]; then
        curl -s "https://get.sdkman.io" | bash
        source "$HOME/.sdkman/bin/sdkman-init.sh"
    fi
    
    # 安装Java 17
    sdk install java 17.0.9-tem
    sdk use java 17.0.9-tem
fi

# 检查Maven
if ! command -v mvn &> /dev/null; then
    echo "Maven未安装，正在安装..."
    
    # 使用SDKMAN安装Maven
    if [ -d "$HOME/.sdkman" ]; then
        source "$HOME/.sdkman/bin/sdkman-init.sh"
        sdk install maven
    else
        echo "请手动安装Maven: brew install maven"
        exit 1
    fi
fi

# 进入项目目录
cd java-backend

# 构建项目
echo "正在构建项目..."
mvn clean package -DskipTests

# 启动应用
echo "正在启动应用..."
mvn spring-boot:run -Dspring-boot.run.profiles=h2
