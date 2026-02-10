# syntax=docker/dockerfile:1.6

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 1️⃣ 先复制 pom
COPY pom.xml .

# 2️⃣ 复制 lib（让 jar 真实存在）
COPY lib ./lib

# 3️⃣ 先 install 本地 SDK（关键）
RUN --mount=type=cache,target=/root/.m2 \
    mvn install:install-file \
      -DgroupId=com.taobao.sdk \
      -DartifactId=taobao-open-sdk \
      -Dversion=0.0.1-SNAPSHOT \
      -Dpackaging=jar \
      -Dfile=lib/tao-taobao-sdk-java-auto_1645365907501-20250413.jar-0.0.1-SNAPSHOT.jar

# 4️⃣ 现在再 go-offline（一定成功）
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B dependency:go-offline

# 5️⃣ 再复制源码
COPY src ./src

# 6️⃣ 构建
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests package
