//package com.nei10u.tip.controller;
//
//import com.alibaba.fastjson2.JSONObject;
//import com.nei10u.tip.service.ActivityService;
//import com.nei10u.tip.vo.ResponseVO;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//@Tag(name = "活动接口")
//@RestController
//@RequestMapping("/api/activity")
//@RequiredArgsConstructor
//public class ActivityController {
//
//    private final ActivityService activityService;
//
//    @Operation(summary = "京东：顶部活动 banner（默认取 2 条）")
//    @GetMapping({ "/jd/banners/top", "/jd/banners/top/" })
//    public ResponseVO<JSONObject> jdTopBanners(@RequestParam(defaultValue = "2") int limit) {
//        return ResponseVO.success(activityService.getJdTopBanners(limit));
//    }
//
//    @Operation(summary = "京东：全部活动 banner（分页）")
//    @GetMapping({ "/jd/banners", "/jd/banners/" })
//    public ResponseVO<JSONObject> jdBanners(@RequestParam(defaultValue = "1") int pageId,
//            @RequestParam(defaultValue = "20") int pageSize) {
//        return ResponseVO.success(activityService.getJdBanners(pageId, pageSize));
//    }
//
//}
