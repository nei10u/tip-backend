package com.nei10u.tip.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nei10u.tip.goods.processor.*;
import com.nei10u.tip.dto.UserDto;
import com.nei10u.tip.exception.BusinessException;
import com.nei10u.tip.mapper.GoodsMapper;
import com.nei10u.tip.model.DtkGoods;
import com.nei10u.tip.service.GoodsService;
import com.nei10u.tip.service.JdConvertService;
import com.nei10u.tip.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, DtkGoods> implements GoodsService {

    private final TbGoodsProcessor tbGoodsProcessor;
    private final TbGoodsSyncProcessor tbGoodsSyncProcessor;
    private final JdGoodsProcessor jdGoodsProcessor;
    private final PddGoodsProcessor pddGoodsProcessor;
    private final VipGoodsProcessor vipGoodsProcessor;
    private final DyGoodsProcessor dyGoodsProcessor;
    private final JdConvertService jdConvertService;
    private final UserService userService;

    @Override
    public JSONObject getTbGoodsList(int pageId, int pageSize) {
        return tbGoodsProcessor.getTbGoodsList(pageId, pageSize);
    }

    @Override
    public JSONObject getTbDailyLowPrice(String sessions, int pageId, int pageSize) {
        return tbGoodsProcessor.getTbDailyLowPrice(sessions, pageId, pageSize);
    }

    @Override
    public JSONObject getTbFreeShippingList(String nineCid, int pageId, int pageSize) {
        return tbGoodsProcessor.getTbFreeShippingList(nineCid, pageId, pageSize);
    }

    @Override
    public JSONObject getTbExplosiveGoodList(String cids, String priceCid, int pageId, int pageSize) {
        return tbGoodsProcessor.getTbExplosiveGoodList(cids, priceCid, pageId, pageSize);
    }

    @Override
    public JSONObject getTbTrend(String type, String cid, int pageId, int pageSize) {
        return tbGoodsProcessor.getTbTrend(type, cid, pageId, pageSize);
    }

    @Override
    public JSONObject getVipHighCommissionGoodsList(int channelType, int sourceType, int pageId, int pageSize) {
        return vipGoodsProcessor.getVipHighCommissionGoodsList(channelType, sourceType, pageId, pageSize);
    }

    @Override
    public JSONObject getVipExplosiveGoodsList(int channelType, int sourceType, int pageId, int pageSize) {
        return vipGoodsProcessor.getVipExplosiveGoodsList(channelType, sourceType, pageId, pageSize);
    }

    /**
     * 京东自营/京东好店API
     */
    @Override
    public JSONObject getJdSelfOperatedGoodsList(String pinPaiName, String pinPai, String cid, String sort, String tj, int pageId, int pageSize) {
        return jdGoodsProcessor.getJdSelfOperatedGoodsList(pinPaiName, pinPai, cid, sort, tj, pageId, pageSize);
    }

    /**
     * 京东精选品牌商品API
     */
    @Override
    public JSONObject getJdSelectedGoodsList(String pinPaiName, String pinPai, String cid, String sort, int pageId, int pageSize) {
        return jdGoodsProcessor.getJdSelectedGoodsList(pinPaiName, pinPai, cid, sort, pageId, pageSize);
    }

    @Override
    public JSONObject getJd30DaysTrend(String pageId, String cid) {
        return jdGoodsProcessor.getJd30DaysTrend(pageId, cid);
    }

    /**
     * @param cid 一级商品分类，值为空：全部商品，1：女装，2：母婴，3：美妆，4：居家日用，5：鞋品，6：美食，7：文娱车品，8：数码家电，
     *            9：男装，10：内衣，11：箱包，12：配饰，13：户外运动，14：家装家纺
     */
    @Override
    public JSONObject getJdCurrentTrend(int cid, int pageId, int pageSize) {
        return jdGoodsProcessor.getJdCurrentTrend(cid, pageId, pageSize);
    }

    @Override
    public JSONObject getPddGoodsList(String keyword, int pageId, int pageSize) {
        return pddGoodsProcessor.getPddGoodsList(keyword, pageId, pageSize);
    }

    @Override
    public JSONObject getPddExplosiveGoodList(int pageId, int pageSize) {
        return pddGoodsProcessor.getPddExplosiveGoodList(pageId, pageSize);
    }

    @Override
    public JSONObject getGoodsDetail(String platform, String goodsId, String userId) {
        if ("tb".equals(platform) || "taobao".equals(platform)) {
            return tbGoodsProcessor.getTbGoodsDetail(goodsId);
        }
        if ("jd".equals(platform) || "jingdong".equals(platform)) {
            return jdGoodsProcessor.getJdGoodsDetail(goodsId);
        }
        if ("pdd".equals(platform) || "pinduoduo".equals(platform)) {
            return pddGoodsProcessor.getPddGoodsDetail(goodsId);
        }
        return new JSONObject();
    }

    @Override
    public JSONObject convertTbLink(String platform, String goodsId, String userId) {
        if ("tb".equals(platform) || "taobao".equals(platform)) {
            return tbGoodsProcessor.convertTbLink(goodsId, userId);
        }
        if ("jd".equals(platform) || "jingdong".equals(platform)) {
            Long uid;
            try {
                uid = Long.parseLong(userId);
            } catch (Exception e) {
                uid = null;
            }
            if (uid == null) {
                throw new BusinessException("INVALID_PARAM", "userId格式错误");
            }
            UserDto u = userService.getUserById(uid);
            String positionId = u == null ? null : u.getJdAuthId();
            if (!StringUtils.hasText(positionId)) {
                throw new BusinessException("JD_NOT_AUTHORIZED", "请先完成京东授权");
            }
            // 京东接口 materialId 支持 skuId（数字）或 url
            return jdConvertService.convertLink(goodsId, positionId);
        }
        if ("dy".equals(platform) || "douyin".equals(platform)) {
            // 抖音转链 external_info 仅允许数字且长度限制更严格；这里不使用 userId，避免触发上游校验失败。
            return dyGoodsProcessor.convert(goodsId, null);
        }
        return new JSONObject();
    }

    @Override
    public JSONObject searchDyGoods(String title, int pageId, int pageSize) {
        return dyGoodsProcessor.search(title, pageId, pageSize);
    }

    @Override
    public JSONObject getDyVideoGoods(Integer cid, String saleNumStart, String sort, int pageId, int pageSize) {
        return dyGoodsProcessor.videoList(cid, saleNumStart, sort, pageId, pageSize);
    }

    @Override
    public void syncGoods() {
        tbGoodsSyncProcessor.syncGoods();
    }

    @Override
    public void syncStaleGoods() {
        tbGoodsSyncProcessor.syncStaleGoods();
    }

    @Override
    public void syncUpdatedGoods() {
        tbGoodsSyncProcessor.syncUpdatedGoods();
    }

    public int cleanupExpiredCouponGoods() {
        return tbGoodsSyncProcessor.cleanupExpiredCouponGoods();
    }
}
