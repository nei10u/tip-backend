package com.nei10u.tip.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nei10u.tip.model.DtkGoods;

/**
 * 商品服务接口
 */
public interface GoodsService extends IService<DtkGoods> {

    /**
     * 唯品会高佣金频道列表
     */
    JSONObject getVipHighCommissionGoodsList(int channelType, int sourceType, int pageId, int pageSize);

    /**
     * 唯品会出单爆款频道列表
     */
    JSONObject getVipExplosiveGoodsList(int channelType, int sourceType, int pageId, int pageSize);


    JSONObject getTbDailyLowPrice(String sessions, int pageId, int pageSize);

    JSONObject getTbFreeShippingList(String nineCid, int pageId, int pageSize);

    /**
     * 每日爆品
     */
    JSONObject getTbExplosiveGoodList(String cids, String priceCid, int pageId, int pageSize);

    /**
     * 获取淘宝商品列表
     */
    JSONObject getTbGoodsList(int pageId, int pageSize);

    /**
     * 获取淘宝榜单
     */
    JSONObject getTbTrend(String type, String cid, int pageId, int pageSize);

    /**
     * 获取京东实时榜单
     */
    JSONObject getJdCurrentTrend(int cid, int pageId, int pageSize);

    /**
     * 京东自营/京东好店API
     */
    JSONObject getJdSelfOperatedGoodsList(String pinPaiName, String pinPai, String cid, String sort, String tj, int pageId, int pageSize);

    /**
     * 京东精选品牌商品API
     */
    JSONObject getJdSelectedGoodsList(String pinPaiName, String pinPai, String cid, String sort, int pageId, int pageSize);

    /**
     * 获取京东30天榜单（当前版本：占位，后续可接入真实第三方接口）
     */
    JSONObject getJd30DaysTrend(String pageId, String cid);

    /**
     * 获取拼多多商品列表
     */
    JSONObject getPddGoodsList(String keyword, int pageId, int pageSize);

    /**
     * 拼多多：爆款
     */
    JSONObject getPddExplosiveGoodList(int pageId, int pageSize);

    /**
     * 获取淘宝商品详情
     */
    JSONObject getGoodsDetail(String platform, String goodsId, String userId);

    /**
     * 淘宝商品转链
     */
    JSONObject convertTbLink(String platform, String goodsId, String userId);

    /**
     * 抖音：商品搜索（返利页列表）
     */
    JSONObject searchDyGoods(String title, int pageId, int pageSize);

    /**
     * 抖货：视频商品列表（折京客 api_videos.ashx）
     * <p>
     * 当前用于 CMS 配置的“视频列表/视频导购”组件数据源（业务归属：京东视频导购/视频商品）。
     */
    JSONObject getDyVideoGoods(Integer cid, String saleNumStart, String sort, int pageId, int pageSize);

    /**
     * 同步商品数据
     */
    void syncGoods();

    /**
     * 同步失效商品
     */
    void syncStaleGoods();

    /**
     * 同步更新商品
     */
    void syncUpdatedGoods();

    /**
     * 清理优惠券已过期的商品（coupon_end_time < 当前时间）。
     *
     * @return 删除的商品数量
     */
    int cleanupExpiredCouponGoods();

}
