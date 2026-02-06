package com.nei10u.tip.controller;

import com.nei10u.tip.service.HomeService;
import com.nei10u.tip.service.CmsHomeLayoutCacheService;
import com.nei10u.tip.service.CmsRebateConfigCacheService;
import com.nei10u.tip.vo.HomeConfigVO;
import com.nei10u.tip.vo.HomeLayoutConfigVO;
import com.nei10u.tip.vo.PublishRequest;
import com.nei10u.tip.vo.PublishResultVO;
import com.nei10u.tip.vo.ResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 首页配置控制器
 * <p>
 * 提供首页相关的 API 接口。
 * 使用 RESTFul 风格。
 * <p>
 * &#064;Tag(name = "首页配置接口") - OpenAPI/Swagger 注解，用于生成 API 文档分组。
 * &#064;RestController - Spring MVC 注解，组合了 @Controller
 * 和 @ResponseBody，表示该类的所有方法返回值直接序列化为 JSON 响应。
 * &#064;RequestMapping("/api/home") - 定义该控制器所有接口的基础路径。
 * &#064;RequiredArgsConstructor - Lombok 注解，自动生成构造函数注入 final 字段 (homeService)。
 */
@Tag(name = "首页配置接口")
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeConfigController {

    private final HomeService homeService;
    private final CmsHomeLayoutCacheService cmsHomeLayoutCacheService;
    private final CmsRebateConfigCacheService cmsRebateConfigCacheService;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.cms.publish-key:}")
    private String cmsPublishKey;

    /**
     * 获取首页聚合配置
     * <p>
     * 客户端 App 启动或进入首页时调用此接口，一次性获取轮播图、菜单、活动区等所有动态配置数据。
     * 
     * @return ResponseVO<HomeConfigVO> 统一响应结构，data 字段包含 HomeConfigVO 数据。
     *         &#064;Operation(summary = "获取首页配置") - OpenAPI/Swagger 注解，用于描述接口功能。
     *         &#064;GetMapping("/config") - Spring MVC 注解，映射 GET 请求到
     *         /api/home/config。
     */
    @Operation(summary = "获取首页配置")
    @GetMapping("/config")
    public ResponseVO<HomeConfigVO> getHomeConfig() {
        return ResponseVO.success(homeService.getHomeConfig());
    }

    /**
     * 诊断 tip-backend 实际连接的数据源与 home_menu 数据量。
     * 用于排查 CMS/APP 配置不一致时，到底是哪一端连接到了不同 DB 或不同 schema。
     */
    @Operation(summary = "诊断数据库连接与 home_menu 数量（debug）")
    @GetMapping("/debug/db")
    public ResponseVO<Map<String, Object>> debugDb() {
        String db = null;
        String schema = null;
        String searchPath = null;
        Long publicHomeMenuCount = null;
        Long cmsHomeMenuCount = null;
        try {
            db = jdbcTemplate.queryForObject("select current_database()", String.class);
            schema = jdbcTemplate.queryForObject("select current_schema()", String.class);
            searchPath = jdbcTemplate.queryForObject("select current_setting('search_path')", String.class);
        } catch (Exception ignored) {
        }
        try {
            publicHomeMenuCount = jdbcTemplate.queryForObject("select count(1) from public.home_menu", Long.class);
        } catch (Exception ignored) {
        }
        try {
            cmsHomeMenuCount = jdbcTemplate.queryForObject("select count(1) from cms.home_menu", Long.class);
        } catch (Exception ignored) {
        }
        return ResponseVO.success(Map.of(
                "service", "tip-backend",
                "db", db,
                "schema", schema,
                "searchPath", searchPath,
                "count", Map.of(
                        "public.home_menu", publicHomeMenuCount,
                        "cms.home_menu", cmsHomeMenuCount
                )
        ));
    }

    @Operation(summary = "获取首页 layout 编排配置（tip-backend 缓存）")
    @GetMapping("/layout-config")
    public ResponseVO<HomeLayoutConfigVO> getLayoutConfig() {
        return ResponseVO.success(cmsHomeLayoutCacheService.getCachedOrRefresh());
    }

    /**
     * 服务启动完成后：从数据库读取一次 layout config 刷新缓存。
     * 对齐“先写库、发布刷新缓存”的模式，也避免首次请求时才触发加载。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmupHomeLayoutCacheOnStartup() {
        try {
            cmsHomeLayoutCacheService.refreshFromDb();
        } catch (Exception ignored) {
        }
    }

    @Operation(summary = "刷新首页 layout 编排缓存（由 CMS 发布触发）")
    @PostMapping("/layout-config/refresh")
    public ResponseVO<HomeLayoutConfigVO> refreshLayoutConfig(
            @RequestHeader(value = "X-Cms-Publish-Key", required = false) String key) {
        // 允许通过配置关闭鉴权（publish-key 为空则不校验）
        final String requiredKey = cmsPublishKey == null ? "" : cmsPublishKey.trim();
        if (!requiredKey.isEmpty()) {
            final String actual = key == null ? "" : key.trim();
            if (!requiredKey.equals(actual)) {
                return ResponseVO.error(403, "forbidden");
            }
        }
        return ResponseVO.success(cmsHomeLayoutCacheService.refreshFromCms());
    }

    @Operation(summary = "统一发布（CMS 右上角发布按钮调用，先写库后刷新 tip-backend 缓存）")
    @PostMapping("/publish")
    public ResponseVO<PublishResultVO> publish(
            @RequestHeader(value = "X-Cms-Publish-Key", required = false) String key,
            @RequestBody(required = false) PublishRequest body) {
        final String requiredKey = cmsPublishKey == null ? "" : cmsPublishKey.trim();
        if (!requiredKey.isEmpty()) {
            final String actual = key == null ? "" : key.trim();
            if (!requiredKey.equals(actual)) {
                return ResponseVO.error(403, "forbidden");
            }
        }

        List<String> categories = body == null ? List.of()
                : (body.getCategories() == null ? List.of() : body.getCategories());
        Set<String> set = categories.stream().filter(s -> s != null && !s.isBlank()).map(String::trim)
                .collect(Collectors.toSet());

        PublishResultVO result = new PublishResultVO();
        result.setRequested(categories);

        // 当前已实现可刷新缓存：home_layout
        if (set.contains("home_layout")) {
            cmsHomeLayoutCacheService.refreshFromCms();
            result.getRefreshed().add("home_layout");
        }

        // 返利页配置缓存（按配置的 platform codes 批量刷新）
        if (set.contains("rebate_pages")) {
            // 关键：把最新编辑的页面真正“发布”为 published，否则 tip-cms 的 rebate/pages 查询永远拿不到（只查 published）
            // 注意：这里直接写入 cms.page（同库），避免依赖 tip-cms 管理接口鉴权/网络。
            publishLatestCmsPages();
            cmsRebateConfigCacheService.refreshAll();
            result.getRefreshed().add("rebate_pages");
        }

        // 其它类目目前为“只写库立即生效/或未实现缓存”，先按跳过返回（便于后续扩展）
        for (String c : set) {
            if ("home_layout".equals(c))
                continue;
            if ("rebate_pages".equals(c))
                continue;
            result.getSkipped().add(c);
        }

        return ResponseVO.success(result);
    }

    /**
     * 将每个 platform 下“最新更新时间”的 page 置为 published，其余置为 draft。
     * <p>
     * 设计意图：匹配“点击保存再发布”的直觉语义，避免用户还需要额外手动切换 status。
     * Postgres: DISTINCT ON 语法。
     */
    private void publishLatestCmsPages() {
        try {
            final String latestCte = """
                    with latest as (
                      select distinct on (platform_id) id, platform_id
                      from cms.page
                      order by platform_id, updated_time desc nulls last, id desc
                    )
                    """;

            // 1) 先把所有 page 置为 draft（减少多条 published 的歧义）
            jdbcTemplate.update("update cms.page set status='draft', updated_time=now() where status <> 'draft'");

            // 2) 再把每个平台最新的一条置为 published
            jdbcTemplate.update(latestCte
                    + " update cms.page p set status='published', updated_time=now() from latest l where p.id=l.id");
        } catch (Exception ignored) {
            // ignore：发布流程不应因状态更新失败而阻塞缓存刷新
        }
    }
}
