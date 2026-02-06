package com.nei10u.tip.controller;

import com.alibaba.fastjson2.JSONObject;
import com.nei10u.tip.service.CmsRebateConfigCacheService;
import com.nei10u.tip.vo.ResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "CMS 返利页配置代理接口")
@RestController
@RequestMapping("/api/cms/rebate")
@RequiredArgsConstructor
public class CmsRebateConfigProxyController {

    private final CmsRebateConfigCacheService cmsRebateConfigCacheService;

    @Operation(summary = "获取返利页配置（tip-backend 缓存代理 tip-cms）")
    @GetMapping("/pages/{platformCode}")
    public ResponseVO<JSONObject> getPage(@PathVariable String platformCode) {
        JSONObject data = cmsRebateConfigCacheService.getPublishedPage(platformCode);
        if (data == null || data.isEmpty()) {
            return ResponseVO.error(404, "page_not_found");
        }
        return ResponseVO.success(data);
    }
}

