# FROM docker.io/library/eclipse-temurin:17
# LABEL "language"="java"
# LABEL "framework"="spring-boot"

FROM maven:3.9-eclipse-temurin-17 AS build

# # 安装 Maven
# RUN --mount=type=cache,target=/var/cache/apt,sharing=locked \
#     --mount=type=cache,target=/var/lib/apt,sharing=locked \
#     apt update \
#     && apt-get --no-install-recommends install -y \
#     maven \
#     ca-certificates-java

WORKDIR /src

# —————————————— 关键修改部分开始 ——————————————

# 1. 先只复制 pom.xml 和 lib 目录
# 目的：确保在运行 Maven 命令前，容器里的 /src/lib 目录下真实存在那个 jar 包
# 且只要依赖不改，这一层缓存就能被复用，不用每次都重新下载依赖
COPY pom.xml .
COPY lib ./lib

# 2. (可选) 预下载依赖
# 这一步会下载 internet 上的依赖，但本地 lib 的依赖已存在，所以不会报错
RUN mvn dependency:go-offline -B || true

# 3. 复制剩余的所有源代码
# 把这一步放在后面，因为代码改动最频繁
COPY . .

# —————————————— 关键修改部分结束 ——————————————

# 4. 执行构建
# 建议使用 package 而非 install，Docker 容器内通常不需要安装到本地仓库
RUN mvn clean package -Dmaven.test.skip=true

# 启动命令
CMD java -Dserver.port=$PORT -jar target/*.jar