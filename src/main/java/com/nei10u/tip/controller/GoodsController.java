package com.nei10u.tip.controller;

import com.alibaba.fastjson2.JSONObject;
import com.nei10u.tip.service.ActivityService;
import com.nei10u.tip.service.GoodsService;
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
public class GoodsController {

    @Autowired
    private final GoodsService goodsService;

    @Autowired
    private final ActivityService activityService;

    /**
     * 首页展示列表
     *
     * @param sessions
     * @param pageId
     * @param pageSize
     */
    @Operation(summary = "获取每日低价抢购")
    @GetMapping("/tb/daily/low")
    public ResponseVO<JSONObject> getDailyLowPriceList(@RequestParam String sessions,
                                                       @RequestParam(defaultValue = "1") int pageId,
                                                       @RequestParam(defaultValue = "20") int pageSize) {
        JSONObject result = goodsService.getTbDailyLowPrice(sessions, pageId, pageSize);
        return ResponseVO.success(result);
    }

    /**
     * 包邮精选
     *
     * @param nineCid  9.9精选的类目id，分类id请求详情：-1:精选，1 :3.9元区，2 :9.9元区，3 :19.9元区
     * @param pageId
     * @param pageSize
     */
    @Operation(summary = "获取包邮精选")
    @GetMapping("/tb/free/shipping")
    public ResponseVO<JSONObject> getFreeShippingList(@RequestParam(defaultValue = "2") String nineCid,
                                                      @RequestParam(defaultValue = "1") int pageId,
                                                      @RequestParam(defaultValue = "20") int pageSize) {
        JSONObject result = goodsService.getTbFreeShippingList(nineCid, pageId, pageSize);
        return ResponseVO.success(result);
    }

    @Operation(summary = "获取商品列表")
    @GetMapping("/tb/list")
    public ResponseVO<JSONObject> getTbGoodsList(
            @RequestParam(defaultValue = "1") int pageId,
            @RequestParam(defaultValue = "20") int pageSize) {
        JSONObject result = goodsService.getTbGoodsList(pageId, pageSize);
        return ResponseVO.success(result);
    }

    @Operation(summary = "获取商品详情")
    @GetMapping("/{platform}/{goodsId}")
    public ResponseVO<JSONObject> getGoodsDetail(
            @PathVariable String platform,
            @PathVariable String goodsId,
            @RequestParam String userId) {
        JSONObject result = goodsService.getGoodsDetail(platform, goodsId, userId);
        return ResponseVO.success(result);
    }

    @Operation(summary = "获取商品详情（query 形式，兼容 goodsId 含 / 的场景）")
    @GetMapping("/detail")
    public ResponseVO<JSONObject> getGoodsDetailByQuery(
            @RequestParam String platform,
            @RequestParam String goodsId,
            @RequestParam String userId) {
        JSONObject result = goodsService.getGoodsDetail(platform, goodsId, userId);
        return ResponseVO.success(result);
    }

    @Operation(summary = "淘宝：商品转链")
    @GetMapping("/tb/convert")
    public ResponseVO<JSONObject> convertTbLink(
            @RequestParam String platform,
            @RequestParam String goodsId,
            @RequestParam String userId) {
        JSONObject result = goodsService.convertTbLink(platform, goodsId, userId);
        return ResponseVO.success(result);
    }

    @Operation(summary = "抖音：商品搜索")
    @GetMapping("/dy/search")
    public ResponseVO<JSONObject> searchDyGoods(@RequestParam(required = false, defaultValue = "") String title,
                                                @RequestParam(defaultValue = "1") int pageId,
                                                @RequestParam(defaultValue = "20") int pageSize) {
        JSONObject result = goodsService.searchDyGoods(title, pageId, pageSize);
        return ResponseVO.success(result);
    }

    @Operation(summary = "抖货：视频商品列表（折京客 api_videos.ashx；当前用于京东视频导购/视频商品）")
    @GetMapping("/dy/videos")
    public ResponseVO<JSONObject> getDyVideoGoods(@RequestParam(required = false) Integer cid,
                                                  @RequestParam(required = false) String saleNumStart,
                                                  @RequestParam(required = false) String sort,
                                                  @RequestParam(defaultValue = "1") int pageId,
                                                  @RequestParam(defaultValue = "20") int pageSize) {
        JSONObject result = goodsService.getDyVideoGoods(cid, saleNumStart, sort, pageId, pageSize);
        return ResponseVO.success(result);
    }

    @Operation(summary = "淘宝：获取商品热榜列表")
    @GetMapping("/tb/trend")
    public ResponseVO<JSONObject> getTbTrend(
            @RequestParam String type,
            @RequestParam(required = false, defaultValue = "") String cid,
            @RequestParam(defaultValue = "1") int pageId,
            @RequestParam(defaultValue = "20") int pageSize) {
        JSONObject result = goodsService.getTbTrend(type, cid, pageId, pageSize);
        return ResponseVO.success(result);
    }

    @Operation(summary = "淘宝：每日爆品推荐")
    @GetMapping("/tb/explosive")
    public ResponseVO<JSONObject> getExplosiveGoodsList(@RequestParam(required = false, defaultValue = "") String cids,
                                                        @RequestParam(required = false, defaultValue = "") String priceCid,
                                                        @RequestParam(defaultValue = "1") int pageId,
                                                        @RequestParam(defaultValue = "20") int pageSize) {
        JSONObject result = goodsService.getTbExplosiveGoodList(cids, priceCid, pageId, pageSize);
        return ResponseVO.success(result);
    }

    @Operation(summary = "唯品会：获取高佣金频道商品列表")
    @GetMapping("/vip/hc/list")
    public ResponseVO<JSONObject> getVipHighCommissionGoodsList(
            @RequestParam(defaultValue = "1") int pageId,
            @RequestParam(defaultValue = "20") int pageSize) {
        JSONObject result = goodsService.getVipHighCommissionGoodsList(0, 0, pageId, pageSize);
        return ResponseVO.success(result);
    }

    @Operation(summary = "唯品会：获取出单爆款商品列表")
    @GetMapping("/vip/explosive")
    public ResponseVO<JSONObject> getVipExplosiveGoodsList(
            @RequestParam(defaultValue = "1") int pageId,
            @RequestParam(defaultValue = "20") int pageSize) {
        JSONObject result = goodsService.getVipExplosiveGoodsList(1, 0, pageId, pageSize);
        return ResponseVO.success(result);
    }

    //
    // @Operation(summary = "获取淘宝榜单")
    // @GetMapping("/trend/tb")
    // public ResponseVO<JSONObject> getTbTrend(
    // @RequestParam(defaultValue = "1") String type,
    // @RequestParam(defaultValue = "1") String pageId,
    // @RequestParam(required = false) String cid,
    // @RequestParam(defaultValue = "20") int pageSize) {
    // JSONObject result = goodsService.getTbTrend(type, pageId, cid, pageSize);
    // return ResponseVO.success(result);
    // }
    //
    @Operation(summary = "京东：获取实时榜单")
    @GetMapping("/jd/current/trend")
    public ResponseVO<JSONObject> getJdCurrentTrend(
            @RequestParam(defaultValue = "0") int cid,
            @RequestParam(defaultValue = "1") int pageId,
            @RequestParam(defaultValue = "20") int pageSize) {
        JSONObject result = goodsService.getJdCurrentTrend(cid, pageId, pageSize);
        return ResponseVO.success(result);
    }

    @Operation(summary = "京东：获取京东精选")
    @GetMapping("/jd/selected")
    public ResponseVO<JSONObject> getJdSelectedGoodsList(
            @RequestParam(required = false) String pinPaiName,
            @RequestParam(required = false) String pinPai,
            @RequestParam(required = false) String cid,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") int pageId,
            @RequestParam(defaultValue = "20") int pageSize) {
        JSONObject result = goodsService.getJdSelectedGoodsList(pinPaiName, pinPai, cid, sort, pageId, pageSize);
        return ResponseVO.success(result);
    }

    @Operation(summary = "京东：获取京东自营好店")
    @GetMapping("/jd/self/operated")
    public ResponseVO<JSONObject> getJdSelfOperatedGoodsList(@RequestParam(required = false) String pinPaiName,
                                                             @RequestParam(required = false) String pinPai,
                                                             @RequestParam(required = false) String cid,
                                                             @RequestParam(required = false) String sort,
                                                             @RequestParam(required = false) String tj,
                                                             @RequestParam(defaultValue = "1") int pageId,
                                                             @RequestParam(defaultValue = "20") int pageSize) {
        JSONObject result = goodsService.getJdSelfOperatedGoodsList(pinPaiName, pinPai, cid, sort, tj, pageId,
                pageSize);
        return ResponseVO.success(result);
    }

    @Operation(summary = "京东：获取京东30天榜单")
    @GetMapping("/jd/30days/trend")
    public ResponseVO<JSONObject> getJd30DaysTrend(
            @RequestParam(defaultValue = "1") String pageId,
            @RequestParam(required = false) String cid) {
        JSONObject result = goodsService.getJd30DaysTrend(pageId, cid);
        return ResponseVO.success(result);
    }

    @Operation(summary = "拼多多：拼多多爆款精选")
    @GetMapping("/pdd/explosive")
    public ResponseVO<JSONObject> getPddExplosiveGoodList(
            @RequestParam(defaultValue = "1") int pageId,
            @RequestParam(defaultValue = "20") int pageSize) {
        JSONObject result = goodsService.getPddExplosiveGoodList(pageId, pageSize);
        return ResponseVO.success(result);
    }

    @Operation(summary = "拼多多：获取拼多多商品列表")
    @GetMapping("/pdd/list")
    public ResponseVO<JSONObject> getPddGoodsList(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int pageId,
            @RequestParam(defaultValue = "20") int pageSize) {
        JSONObject result = goodsService.getPddGoodsList(keyword, pageId, pageSize);
        return ResponseVO.success(result);
    }

    @Operation(summary = "拼多多：获取拼多多商品详情")
    @GetMapping("/pdd/detail")
    public ResponseVO<JSONObject> getPddGoodsDetail(
            @RequestParam String goodsId,
            @RequestParam(required = false, defaultValue = "0") String userId) {
        // 统一复用 goodsDetail 聚合接口，避免重复一套签名
        JSONObject result = goodsService.getGoodsDetail("pdd", goodsId, userId);
        return ResponseVO.success(result);
    }

    @Operation(summary = "抖音：活动列表banner（默认取 2 条））")
    @GetMapping("/dy/banners/top")
    public ResponseVO<JSONObject> dyActivitiesBanners(@RequestParam(defaultValue = "2") int limit) {
        return ResponseVO.success(activityService.getDyActivitiesBanners(limit));
    }

    @Operation(summary = "抖音：活动列表")
    @GetMapping("/dy/activities")
    public ResponseVO<JSONObject> dyActivities(@RequestParam(defaultValue = "1") int pageId,
                                               @RequestParam(defaultValue = "20") int pageSize,
                                               @RequestParam(required = false) Integer activityStatus) {
        return ResponseVO.success(activityService.getDyActivities(pageId, pageSize, activityStatus));
    }

    @Operation(summary = "抖音：活动转链（折淘客）")
    @GetMapping("/dy/activity/convert")
    public ResponseVO<JSONObject> dyActivityConvert(@RequestParam String materialId,
                                                    @RequestParam(required = false) String externalInfo,
                                                    @RequestParam(required = false) Boolean needQrCode) {
        return ResponseVO.success(activityService.convertDyActivity(materialId, externalInfo, needQrCode));
    }
}
