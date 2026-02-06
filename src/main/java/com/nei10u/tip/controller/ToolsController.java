package com.nei10u.tip.controller;

import com.alibaba.fastjson2.JSONObject;
import com.nei10u.tip.model.PlatformEnum;
import com.nei10u.tip.service.JdConvertService;
import com.nei10u.tip.vo.ResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "工具接口")
@RestController
@RequestMapping("/api/tools")
@RequiredArgsConstructor
public class ToolsController {

    @Autowired
    private JdConvertService jdConvertService;

    @Operation(summary = "页面转链")
    @PostMapping("/link/convert")
    public ResponseVO<JSONObject> linkConvert(@RequestParam String platform,
                                              @RequestParam String materialId,
                                              @RequestParam String positionId) {
        PlatformEnum platformEnum = PlatformEnum.valueOf(platform);
        if (PlatformEnum.JD.equals(platformEnum)) {
            JSONObject result = jdConvertService.convertLink(materialId, positionId);
            return ResponseVO.success(result);
        }
        return ResponseVO.error("转换失败");
    }
}
