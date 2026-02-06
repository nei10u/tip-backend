package com.nei10u.tip.goods.processor;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nei10u.tip.exception.BusinessException;
import com.nei10u.tip.mapper.GoodsMapper;
import com.nei10u.tip.mapper.PromotionInfoMapper;
import com.nei10u.tip.model.DtkGoods;
import com.nei10u.tip.model.PromotionInfo;
import com.nei10u.tip.service.DtkApiService;
import com.nei10u.tip.service.GoodsCacheService;
import com.nei10u.tip.service.TbConvertService;
import com.nei10u.tip.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static com.nei10u.tip.service.impl.GoodsCacheServiceImpl.HOME_LIST_PAGE_ID;
import static com.nei10u.tip.service.impl.GoodsCacheServiceImpl.HOME_LIST_PAGE_SIZE;
import static com.nei10u.tip.service.impl.GoodsCacheServiceImpl.HOME_TB_TREND_TYPE;

/**
 * 淘宝（TB）平台商品处理器：负责接入 DTK & 本地库存商品 & TB 转链。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TbGoodsProcessor {

    private final GoodsMapper goodsMapper;
    private final DtkApiService dtkApiService;
    private final GoodsCacheService goodsCacheService;
    private final TbConvertService tbConvertService;
    private final PromotionInfoMapper promotionInfoMapper;
    private final UserService userService;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public JSONObject getTbGoodsList(int pageId, int pageSize) {
        Page<DtkGoods> page = new Page<>(pageId, pageSize);
        goodsMapper.selectPage(page, new LambdaQueryWrapper<DtkGoods>()
                .eq(DtkGoods::getStatus, 1)
                .orderByDesc(List.of(DtkGoods::getUpdateTime, DtkGoods::getCommissionRate)));

        JSONObject result = new JSONObject();
        result.put("list", page.getRecords());
        result.put("totalNum", page.getTotal());
        result.put("pageId", String.valueOf(pageId));
        return result;
    }

    public JSONObject getTbDailyLowPrice(String sessions, int pageId, int pageSize) {
        String response = dtkApiService.getDailyLowPrice(sessions, pageId, pageSize);
        return StringUtils.hasText(response) ? JSON.parseObject(response) : new JSONObject();
    }

    public JSONObject getTbFreeShippingList(String nineCid, int pageId, int pageSize) {
        String response = dtkApiService.getFreeShippingList(nineCid, pageId, pageSize);
        return StringUtils.hasText(response) ? JSON.parseObject(response) : new JSONObject();
    }

    public JSONObject getTbExplosiveGoodList(String cids, String priceCid, int pageId, int pageSize) {
        String response = dtkApiService.getExplosiveGoodList(cids, priceCid, pageId, pageSize);
        return StringUtils.hasText(response) ? JSON.parseObject(response) : new JSONObject();
    }

    public JSONObject getTbTrend(String type, String cid, int pageId, int pageSize) {
        if (HOME_LIST_PAGE_ID == pageId
                && HOME_LIST_PAGE_SIZE == pageSize
                && HOME_TB_TREND_TYPE.equals(type)
                && !StringUtils.hasText(cid)) {
            return goodsCacheService.getOrLoadTbTrendFirstPage();
        }
        return goodsCacheService.fetchTbTrend(type, cid, pageId, pageSize);
    }

    /**
     * TB 商品详情：沿用原逻辑（补算佣金、解析图片字段）
     */
    public JSONObject getTbGoodsDetail(String goodsId) {
        String response = dtkApiService.getGoodsDetails(goodsId);
        if (!StringUtils.hasText(response)) return new JSONObject();

        JSONObject json = JSON.parseObject(response);
        if (json != null && json.containsKey("data")) {
            JSONObject data = json.getJSONObject("data");
            if (data != null) {
                BigDecimal existingCommission = data.getBigDecimal("commission");
                if (existingCommission == null) {
                    BigDecimal estimateAmount = data.getBigDecimal("estimateAmount");
                    if (estimateAmount == null) {
                        BigDecimal price = data.getBigDecimal("actualPrice");
                        if (price == null) price = data.getBigDecimal("price");
                        if (price == null) price = data.getBigDecimal("originalPrice");

                        BigDecimal rate = data.getBigDecimal("commissionRate");
                        if (rate == null && data.containsKey("commissionRate")) {
                            Object raw = data.get("commissionRate");
                            try {
                                if (raw != null) rate = new BigDecimal(raw.toString());
                            } catch (Exception ignore) {
                            }
                        }

                        BigDecimal divisor = data.getBigDecimal("divisor");
                        if (divisor == null || divisor.compareTo(BigDecimal.ZERO) <= 0) {
                            divisor = BigDecimal.ONE;
                        }

                        if (price != null && rate != null) {
                            BigDecimal commission = price
                                    .multiply(rate)
                                    .divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP)
                                    .divide(divisor, 2, RoundingMode.HALF_UP);
                            data.put("commission", commission);
                        }
                    } else {
                        data.put("commission", estimateAmount);
                    }
                }

                String detailImages = data.getString("detailPics");
                if (StringUtils.hasText(detailImages)) {
                    data.put("detailPics", List.of(detailImages.split(",")));
                }
                String carouselImages = data.getString("imgs");
                if (StringUtils.hasText(carouselImages)) {
                    data.put("carouselPics", List.of(carouselImages.split(",")));
                }
                String relatedImages = data.getString("reimgs");
                if (StringUtils.hasText(relatedImages)) {
                    data.put("relatedPics", List.of(relatedImages.split(",")));
                }
            }
        }
        return json == null ? new JSONObject() : json;
    }

    /**
     * 淘宝商品转链（TB 专属）
     */
    public JSONObject convertTbLink(String goodsId, String userId) {
        Long uid;
        try {
            uid = Long.parseLong(userId);
        } catch (Exception e) {
            uid = null;
        }
        if (uid == null) {
            throw new BusinessException("INVALID_PARAM", "userId格式错误");
        }

        PromotionInfo pi = new PromotionInfo();
        pi.setGoodsId(goodsId);
        pi.setPlatform("tb");
        pi.setUserId(uid);
        pi.setExternalId(UUID.randomUUID().toString().replace("-", ""));
        pi.setCreateTime(new java.util.Date());
        promotionInfoMapper.insert(pi);

        String relationId = null;
        String specialId = null;
        com.nei10u.tip.dto.UserDto u = userService.getUserById(uid);
        if (u != null) {
            if (u.getRelationId() != null) relationId = String.valueOf(u.getRelationId());
            if (u.getSpecialId() != null) specialId = String.valueOf(u.getSpecialId());
        }
        if (!StringUtils.hasText(relationId) && !StringUtils.hasText(specialId)) {
            throw new BusinessException("TB_NOT_AUTHORIZED", "请先完成淘宝授权");
        }

        JSONObject convert = tbConvertService.convert(goodsId, relationId, specialId, null, pi.getExternalId());
        String promotionUrl = convert.getString("shortUrl");
        if (!StringUtils.hasText(promotionUrl)) {
            promotionUrl = convert.getString("longUrl");
        }
        if (StringUtils.hasText(promotionUrl)) {
            pi.setPromotionUrl(promotionUrl);
            promotionInfoMapper.updateById(pi);
        }

        convert.put("promotionInfoId", pi.getId());
        convert.put("externalId", pi.getExternalId());
        convert.put("promotionUrl", promotionUrl);
        convert.put("redirectUrl", "/api/promo/redirect/" + pi.getId());
        return convert;
    }
}

