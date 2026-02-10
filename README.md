# Tip Backend

基于 Spring Boot 3 + MyBatis Plus 的 CPS 后端系统。
《来运桔》

## 功能模块

- **用户系统**: 注册、登录、CPS ID绑定
- **订单系统**: 订单同步、查询、统计
- **商品系统**: 多平台(淘宝/京东/拼多多)榜单、商品详情、转链
- **资金系统**: 佣金管理、提现

## 技术栈

- Spring Boot 3.1.5
- MyBatis Plus 3.5.4.1
- PostgreSQL
- Redis
- Lombok
- FastJSON2

## 快速开始

1. **配置数据库**
   修改 `src/main/resources/application.yml` 中的数据库连接信息。

2. **配置Redis**
   确保本地 Redis 服务已启动。

3. **配置大淘客API**
   在 `application.yml` 中配置 `app.dtk.app-key` 和 `app.dtk.app-secret`。

4. **本地执行**
   首次执行
   ```bash
   mvn install:install-file \
   -DgroupId=com.taobao.sdk \
   -DartifactId=taobao-open-sdk \
   -Dversion=0.0.1-SNAPSHOT \
   -Dpackaging=jar \
   -Dfile=lib/tao-taobao-sdk-java-auto_1645365907501-20250413.jar-0.0.1-SNAPSHOT.jar
   ```
   
5. **运行**
   ```bash
   mvn spring-boot:run
   ```
   应用启动时会自动创建数据库表结构。

## API 文档

启动后访问: http://localhost:8080/swagger-ui.html

## 定时任务

- 订单同步: 每15分钟执行一次
