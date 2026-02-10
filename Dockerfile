# syntax=docker/dockerfile:1.6

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 1️⃣ 先复制 pom
COPY pom.xml .

# 2️⃣ 复制 lib
COPY lib ./lib

# 3️⃣ install 本地 SDK
# 修复点：
# 1. 确保 mvn 命令后的参数紧凑，避免行尾空格导致的换行失效
# 2. 给 -Dfile 路径加上双引号，防止文件名中的特殊字符引发解析问题
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B install:install-file \
    -DgroupId=com.taobao.sdk \
    -DartifactId=taobao-open-sdk \
    -Dversion=0.0.1-SNAPSHOT \
    -Dpackaging=jar \
    "-Dfile=lib/tao-taobao-sdk-java-auto_1645365907501-20250413.jar-0.0.1-SNAPSHOT.jar"

# 4️⃣ 预解析依赖
# 修复点：确保 goal (dependency:go-offline) 明确且没有被换行符截断
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests validate dependency:go-offline

# 5️⃣ 再复制源码
COPY src ./src

# 6️⃣ 构建
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests clean package