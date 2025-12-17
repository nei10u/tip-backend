package com.nei10u.tip.controller;

import com.alibaba.fastjson2.JSONObject;
import com.nei10u.tip.service.StatisticsService;
import com.nei10u.tip.vo.ResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 数据统计控制器
 */
@Tag(name = "数据统计接口")
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "获取用户统计概览")
    @GetMapping("/summary")
    public ResponseVO<JSONObject> getUserSummary(@RequestParam Long userId) {
        JSONObject result = statisticsService.getUserSummary(userId);
        return ResponseVO.success(result);
    }
}
