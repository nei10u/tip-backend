# syntax=docker/dockerfile:1.6

############################
# 1️⃣ 构建阶段
############################
FROM maven:3.9-eclipse-temurin-17 AS build
LABEL language="java"
LABEL framework="spring-boot"

WORKDIR /app

# —— ① 只复制 pom.xml（最大化依赖缓存）——
COPY pom.xml .

# 预下载依赖（强缓存层）
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B dependency:go-offline

# —— ② 复制 lib 并安装本地 jar 到 Maven 仓库 ——
COPY lib ./lib
RUN --mount=type=cache,target=/root/.m2 \
    mvn install:install-file \
      -DgroupId=com.taobao.sdk \
      -DartifactId=taobao-open-sdk \
      -Dversion=0.0.1 \
      -Dpackaging=jar \
      -Dfile=lib/tao-taobao-sdk-java-auto_1645365907501-20250413.jar-0.0.1-SNAPSHOT.jar

# —— ③ 再复制源码（代码变动不影响依赖层）——
COPY src ./src

# 构建
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests package


############################
# 2️⃣ 运行阶段（极小镜像）
############################
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

CMD ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]