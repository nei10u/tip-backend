package com.nei10u.tip.controller;

import com.nei10u.tip.service.HomeService;
import com.nei10u.tip.vo.HomeConfigVO;
import com.nei10u.tip.vo.ResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页配置控制器
 * <p>
 * 提供首页相关的 API 接口。
 * 使用 RESTFul 风格。
 * <p>
 * &#064;Tag(name  = "首页配置接口") - OpenAPI/Swagger 注解，用于生成 API 文档分组。
 * &#064;RestController  - Spring MVC 注解，组合了 @Controller
 *                 和 @ResponseBody，表示该类的所有方法返回值直接序列化为 JSON 响应。
 *                 &#064;RequestMapping("/api/home")  - 定义该控制器所有接口的基础路径。
 * &#064;RequiredArgsConstructor  - Lombok 注解，自动生成构造函数注入 final 字段 (homeService)。
 */
@Tag(name = "首页配置接口")
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeConfigController {

    private final HomeService homeService;

    /**
     * 获取首页聚合配置
     * <p>
     * 客户端 App 启动或进入首页时调用此接口，一次性获取轮播图、菜单、活动区等所有动态配置数据。
     * 
     * @return ResponseVO<HomeConfigVO> 统一响应结构，data 字段包含 HomeConfigVO 数据。
     * &#064;Operation(summary  = "获取首页配置") - OpenAPI/Swagger 注解，用于描述接口功能。
     * &#064;GetMapping("/config")  - Spring MVC 注解，映射 GET 请求到 /api/home/config。
     */
    @Operation(summary = "获取首页配置")
    @GetMapping("/config")
    public ResponseVO<HomeConfigVO> getHomeConfig() {
        return ResponseVO.success(homeService.getHomeConfig());
    }
}
