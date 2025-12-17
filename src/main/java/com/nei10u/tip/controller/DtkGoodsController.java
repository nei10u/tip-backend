package com.nei10u.tip.controller;

import com.alibaba.fastjson2.JSONObject;
import com.nei10u.tip.service.DtkGoodsService;
import com.nei10u.tip.service.ZtkGoodsService;
import com.nei10u.tip.vo.ResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 商品控制器
 */
@Tag(name = "商品接口")
@RestController
@RequestMapping("/api/goods")
@RequiredArgsConstructor
public class DtkGoodsController {

    @Autowired
    private final DtkGoodsService dtkGoodsService;

    @Autowired
    private final ZtkGoodsService ztkGoodsService;

    @Operation(summary = "获取商品列表")
    @GetMapping("/list")
    public ResponseVO<JSONObject> getGoodsList(
            @RequestParam(defaultValue = "1") int pageId,
            @RequestParam(defaultValue = "20") int pageSize) {
        JSONObject result = dtkGoodsService.getGoodsList(pageId, pageSize);
        return ResponseVO.success(result);
    }

    @Operation(summary = "获取唯品会商品列表")
    @GetMapping("/vip/list")
    public ResponseVO<JSONObject> getVipGoodsList(
            @RequestParam(defaultValue = "1") int pageId,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        JSONObject result = ztkGoodsService.getVipGoodsList(pageId, pageSize, keyword);
        return ResponseVO.success(result);
    }

    @Operation(summary = "获取淘宝榜单")
    @GetMapping("/trend/tb")
    public ResponseVO<JSONObject> getTbTrend(
            @RequestParam(defaultValue = "1") String type,
            @RequestParam(defaultValue = "1") String pageId,
            @RequestParam(required = false) String cid,
            @RequestParam(defaultValue = "20") int pageSize) {
        JSONObject result = dtkGoodsService.getTbTrend(type, pageId, cid, pageSize);
        return ResponseVO.success(result);
    }

    @Operation(summary = "获取京东实时榜单")
    @GetMapping("/trend/jd/current")
    public ResponseVO<JSONObject> getJdCurrentTrend(
            @RequestParam(defaultValue = "1") String pageId,
            @RequestParam(required = false) String cid) {
        JSONObject result = dtkGoodsService.getJdCurrentTrend(pageId, cid);
        return ResponseVO.success(result);
    }

    @Operation(summary = "获取京东30天榜单")
    @GetMapping("/trend/jd/30days")
    public ResponseVO<JSONObject> getJd30DaysTrend(
            @RequestParam(defaultValue = "1") String pageId,
            @RequestParam(required = false) String cid) {
        JSONObject result = dtkGoodsService.getJd30DaysTrend(pageId, cid);
        return ResponseVO.success(result);
    }

    @Operation(summary = "获取拼多多榜单")
    @GetMapping("/trend/pdd")
    public ResponseVO<JSONObject> getPddTrend(
            @RequestParam(defaultValue = "1") String type,
            @RequestParam(defaultValue = "1") String pageId,
            @RequestParam(required = false) String cid) {
        JSONObject result = dtkGoodsService.getPddTrend(type, pageId, cid);
        return ResponseVO.success(result);
    }

    @Operation(summary = "获取商品详情")
    @GetMapping("/{platform}/{goodsId}")
    public ResponseVO<JSONObject> getGoodsInfo(
            @PathVariable String platform,
            @PathVariable String goodsId,
            @RequestParam String userId) {
        JSONObject result = dtkGoodsService.getInfo(platform, goodsId, userId);
        return ResponseVO.success(result);
    }

    @Operation(summary = "商品转链")
    @GetMapping("/convert")
    public ResponseVO<JSONObject> convertLink(
            @RequestParam String platform,
            @RequestParam String goodsId,
            @RequestParam String userId) {
        JSONObject result = dtkGoodsService.convertLink(platform, goodsId, userId);
        return ResponseVO.success(result);
    }
}
