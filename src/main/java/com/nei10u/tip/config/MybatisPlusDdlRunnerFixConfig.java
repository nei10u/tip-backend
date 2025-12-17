package com.nei10u.tip.config;

import com.baomidou.mybatisplus.autoconfigure.DdlApplicationRunner;
import com.baomidou.mybatisplus.extension.ddl.IDdl;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 修复 MyBatis-Plus 3.5.4.1：当容器中不存在任何 {@link IDdl} 实现时，
 * {@code MybatisPlusAutoConfiguration#ddlApplicationRunner(...)} 会返回 null，
 * 在 Spring Boot 3.2 的 Runner 收集阶段会表现为 NullBean 并触发类型不匹配异常。
 *
 * 这里提供一个“永不返回 null”的同名 Bean（同类型），让自动配置的条件不再命中。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(DdlApplicationRunner.class)
public class MybatisPlusDdlRunnerFixConfig {

    @Bean
    @ConditionalOnMissingBean(DdlApplicationRunner.class)
    public DdlApplicationRunner ddlApplicationRunner(List<IDdl> ddlList) {
        // Spring 会注入空 List；绝不返回 null，避免被注册为 NullBean
        return new DdlApplicationRunner(ddlList);
    }
}
