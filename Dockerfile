# syntax=docker/dockerfile:1.6

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 1️⃣ 先复制 pom
COPY pom.xml .

# 2️⃣ 复制 lib
COPY lib ./lib

# 3️⃣ install 本地 SDK
# 【关键修改】这里改成了单行命令，并给 file 路径加了引号
# 之前的报错通常是因为反斜杠 \ 后面有隐形空格，或者文件编码(CRLF)导致换行失败
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B install:install-file -DgroupId=com.taobao.sdk -DartifactId=taobao-open-sdk -Dversion=0.0.1-SNAPSHOT -Dpackaging=jar "-Dfile=lib/tao-taobao-sdk-java-auto_1645365907501-20250413.jar-0.0.1-SNAPSHOT.jar"

# 4️⃣ 预解析依赖
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests validate dependency:go-offline

# 5️⃣ 再复制源码
COPY src ./src

# 6️⃣ 构建
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests clean package