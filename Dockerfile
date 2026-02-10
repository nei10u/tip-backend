# syntax=docker/dockerfile:1.6

# 1. 依然使用官方 Maven 镜像（比参考代码中手动 apt install maven 更快、更标准）
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 2. 先复制 pom 和 lib
# 这一步利用 Docker 缓存：只要 pom 和 lib 没变，后续步骤都不会重新跑
COPY pom.xml .
COPY lib ./lib

# 3. 安装本地 SDK 到容器内的 Maven 仓库
# 【核心修复】：
#  - 将命令写成一行，彻底根除 "No goals specified" 错误
#  - 给文件名加上双引号，防止特殊字符报错
RUN --mount=type=cache,target=/root/.m2 \
    mvn install:install-file -B \
    -DgroupId=com.taobao.sdk \
    -DartifactId=taobao-open-sdk \
    -Dversion=0.0.1-SNAPSHOT \
    -Dpackaging=jar \
    "-Dfile=lib/tao-taobao-sdk-java-auto_1645365907501-20250413.jar-0.0.1-SNAPSHOT.jar"

# 4. 预下载互联网依赖
# 【参考代码迁移】：加上 "|| true"，如果某些依赖下载失败（比如 snapshot）不中断构建
# 这样跟你的参考代码逻辑保持一致
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B || true

# 5. 复制源代码
COPY src ./src

# 6. 执行构建
# 跳过测试，确保快速构建
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests

# ——————————————————————————————————————————————
# (可选) 如果你需要运行时镜像，可以多加一个 stage
# FROM eclipse-temurin:17-jre
# WORKDIR /app
# COPY --from=build /app/target/*.jar app.jar
# CMD ["java", "-jar", "app.jar"]