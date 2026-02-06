package com.nei10u.tip.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * 给 tip-cms 提供的元数据接口：动态读取 GoodsController 等暴露的接口列表，
 * 用于 CMS 里的 “dataSourceKey（GoodsController 接口）- 选择预置” 下拉。
 */
@Tag(name = "商品接口元数据")
@Controller
@ResponseBody
@RequestMapping("/api/goods/meta")
@RequiredArgsConstructor
public class GoodsMetaController {

    private final RequestMappingHandlerMapping handlerMapping;

    public record Endpoint(String method, String path, String summary) {
    }

    @Operation(summary = "列出 /api/goods 下所有可用接口")
    @GetMapping("/endpoints")
    public List<Endpoint> endpoints() {
        List<Endpoint> out = new ArrayList<>();

        handlerMapping.getHandlerMethods().forEach((info, handler) -> {
            List<String> paths = extractPaths(info);
            if (paths.isEmpty()) return;

            // 只暴露 /api/goods/** 的接口
            if (paths.stream().noneMatch(p -> p != null && p.startsWith("/api/goods"))) return;

            Set<String> methods = info.getMethodsCondition().getMethods().stream()
                    .map(Enum::name)
                    .collect(java.util.stream.Collectors.toSet());
            if (methods.isEmpty()) {
                // 未显式指定 method：按 ALL 展示（CMS 主要用 GET）
                methods = Set.of("ALL");
            }

            String summary = readOperationSummary(handler);
            for (String p : paths) {
                if (p == null || !p.startsWith("/api/goods")) continue;
                for (String m : methods) {
                    out.add(new Endpoint(m, p, summary));
                }
            }
        });

        out.sort(Comparator
                .comparing(Endpoint::path)
                .thenComparing(Endpoint::method));
        return out;
    }

    private static String readOperationSummary(HandlerMethod handler) {
        if (handler == null) return "";
        Operation op = handler.getMethodAnnotation(Operation.class);
        if (op == null) return "";
        String s = op.summary();
        return s == null ? "" : s.trim();
    }

    private static List<String> extractPaths(RequestMappingInfo info) {
        if (info == null) return List.of();
        try {
            // Spring Boot 3.x 默认 PathPattern
            if (info.getPathPatternsCondition() != null) {
                return new ArrayList<>(info.getPathPatternsCondition().getPatternValues());
            }
        } catch (Exception ignore) {
        }
        try {
            if (info.getPatternsCondition() != null) {
                return new ArrayList<>(info.getPatternsCondition().getPatterns());
            }
        } catch (Exception ignore) {
        }
        return List.of();
    }
}

