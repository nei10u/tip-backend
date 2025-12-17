package com.nei10u.tip.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nei10u.tip.model.DtkGoods;

/**
 * 商品服务接口
 */
public interface DtkGoodsService extends IService<DtkGoods> {
    
    /**
     * 获取商品列表
     */
    JSONObject getGoodsList(int pageId, int pageSize);

    /**
     * 获取淘宝榜单
     */
    JSONObject getTbTrend(String type, String pageId, String cid, int pageSize);

    /**
     * 获取京东实时榜单
     */
    JSONObject getJdCurrentTrend(String pageId, String cid);

    /**
     * 获取京东30天榜单
     */
    JSONObject getJd30DaysTrend(String pageId, String cid);

    /**
     * 获取拼多多榜单
     */
    JSONObject getPddTrend(String type, String pageId, String cid);

    /**
     * 获取商品详情
     */
    JSONObject getInfo(String platform, String goodsId, String userId);

    /**
     * 商品转链
     */
    JSONObject convertLink(String platform, String goodsId, String userId);

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
}
