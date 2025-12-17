package com.nei10u.tip.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * MyBatis Plus 配置类
 * <p>
 * 负责配置 MyBatis Plus 的核心插件和功能。
 * 包括分页插件、乐观锁插件等。
 */
@Configuration // 标识这是一个 Spring 配置类
@EnableTransactionManagement // 开启 Spring 事务管理支持
public class MyBatisPlusConfig {

    /**
     * 注册 MyBatis Plus 拦截器链
     * <p>
     * 这里主要添加了分页拦截器，用于支持数据库的分页查询功能。
     * 
     * @return MybatisPlusInterceptor 拦截器实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 添加分页插件
        // DbType.POSTGRE_SQL 指定数据库类型为 PostgreSQL，
        // 插件会自动生成适合该数据库的分页 SQL (LIMIT/OFFSET)。
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));

        return interceptor;
    }
}
