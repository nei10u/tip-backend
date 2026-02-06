package com.nei10u.tip.service;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

/**
 * 大淘客API服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DtkApiService {

    private final RestTemplate restTemplate;

    @Value("${app.dtk.app-key:6936f422c3a79}")
    private String appKey;

    @Value("${app.dtk.app-secret:b8f60d673b879482b88f21d9e6798df3}")
    private String appSecret;

    @Value("${app.dtk.pid:}")
    private String defaultPid;

    private static final String BASE_URL = "https://openapi.dataoke.com";

    /**
     * 每日低价抢购
     * sessions 1已开始 2预告
     */
    public String getDailyLowPrice(String sessions, int pageId, int pageSize) {
        String url = BASE_URL + "/api/goods/get-half-price-day";
        Map<String, String> params = new TreeMap<>();
        params.put("appKey", appKey);
        params.put("version", "v2.0.0");
        params.put("sessions", String.valueOf(sessions));
        params.put("pageId", String.valueOf(pageId));
        params.put("pageSize", String.valueOf(pageSize));
        return doRequest(url, params);
    }

    /**
     * 9.9包邮精选
     * nineCid
     */
    public String getFreeShippingList(String nineCid, int pageId, int pageSize) {
        String url = BASE_URL + "/api/goods/nine/op-goods-list";
        Map<String, String> params = new TreeMap<>();
        params.put("appKey", appKey);
        params.put("version", "v3.0.0");
        params.put("nineCid", nineCid);
        params.put("pageId", String.valueOf(pageId));
        params.put("pageSize", String.valueOf(pageSize));
        return doRequest(url, params);
    }

    /**
     * 每日爆品
     * 大淘客的一级分类id，如果需要传多个，以英文逗号相隔，如：”1,2,3”。1 -女装，2 -母婴，3 -美妆，4 -居家日用，5 -鞋品，6 -美食，7 -文娱车品，8 -数码家电，9 -男装，10 -内衣，11 -箱包，12 -配饰，13 -户外运动，14 -家装家纺。不填默认全部
     *
     * @param cids     大淘客的一级分类id，如果需要传多个，以英文逗号相隔，如：”1,2,3”。1 -女装，2 -母婴，3 -美妆，4 -居家日用，5 -鞋品，6 -美食，7 -文娱车品，8 -数码家电，9 -男装，10 -内衣，11 -箱包，12 -配饰，13 -户外运动，14 -家装家纺。不填默认全部
     * @param priceCid 价格区间，1表示10~20元区；2表示20~40元区；3表示40元以上区；默认为1
     */
    public String getExplosiveGoodList(String cids, String priceCid, int pageId, int pageSize) {
        String url = BASE_URL + "/api/goods/explosive-goods-list";
        Map<String, String> params = new TreeMap<>();
        params.put("appKey", appKey);
        params.put("version", "v1.0.0");
        if (StringUtils.isNotEmpty(cids)) {
            params.put("cids", cids);
        }
        if (StringUtils.isNotEmpty(priceCid)) {
            params.put("priceCid", priceCid);
        }
        params.put("pageId", String.valueOf(pageId));
        params.put("pageSize", String.valueOf(pageSize));
        return doRequest(url, params);
    }

    /**
     * 获取全量商品列表
     */
    public String getGoodsList(int pageId, int pageSize) {
        String url = BASE_URL + "/api/goods/get-goods-list";
        Map<String, String> params = new TreeMap<>();
        params.put("appKey", appKey);
        params.put("version", "v1.2.3");
        params.put("pageId", String.valueOf(pageId));
        params.put("pageSize", String.valueOf(pageSize));
        return doRequest(url, params);
    }

    /**
     * 获取榜单列表
     * rankType 榜单类型：1.实时榜 ，2.全天榜，3.热推榜，7.综合热搜榜，8.原实时榜2.0（已升级为实时榜），
     * 9.必推榜，11.昨日热销榜，12.7日热销榜，13.30日热销榜，14.去年同期热销榜，15.低价高佣榜。
     * 9以后为2024/05/15新增榜单
     * cid 大淘客一级类目id，1 -女装，2 -母婴，3 -美妆，4 -居家日用，5 -鞋品，6 -美食，
     * 7 -文娱车品，8 -数码家电，9 -男装，10 -内衣，11 -箱包，12 -配饰，13 -户外运动，14 -家装家纺
     * 仅对实时榜单、全天榜单有效
     */
    public String getRankingList(String rankType, String cid, int pageId, int pageSize) {
        String url = BASE_URL + "/api/goods/get-ranking-list";

        Map<String, String> params = new TreeMap<>();
        params.put("rankType", rankType);
        if (StringUtils.isNotEmpty(cid)) {
            params.put("cid", cid);
        }
        params.put("appKey", appKey);
        params.put("pageId", String.valueOf(pageId));
        params.put("pageSize", String.valueOf(pageSize));
        params.put("version", "v1.3.1");

        return doRequest(url, params);
    }

    /**
     * 获取9.9包邮商品
     */
    public String getNineOpGoodsList(int pageId, int pageSize, String cid) {
        String url = BASE_URL + "/api/goods/nine/op-goods-list";

        Map<String, String> params = new TreeMap<>();
        params.put("pageId", String.valueOf(pageId));
        params.put("pageSize", String.valueOf(pageSize));
        if (cid != null && !cid.isEmpty()) {
            params.put("cid", cid);
        }
        params.put("appKey", appKey);
        params.put("version", "v1.2.2");

        return doRequest(url, params);
    }

    /**
     * 猜你喜欢
     *
     * @param tbProductId 淘宝商品id
     * @param size        每页条数，默认10，最大值100
     */
    public String guessYouLike(String tbProductId, int size) {
        String url = BASE_URL + "/api/goods/list-similer-goods-by-open ";
        Map<String, String> params = new TreeMap<>();
        params.put("id", tbProductId);
        if (size > 0) {
            params.put("size", String.valueOf(size));
        }
        params.put("appKey", appKey);
        params.put("version", "v1.2.2");
        return doRequest(url, params);
    }

    /**
     * 获取订单详情 (兼容旧版调用).
     */
    public String getOrderDetails(String startTime, String endTime) {
        return getOrderDetails(startTime, endTime, null);
    }

    /**
     * 获取订单详情 (支持高级查询参数).
     *
     * @param startTime   订单查询开始时间
     * @param endTime     订单查询结束时间
     * @param queryParams 其他查询参数 (如 queryType, positionIndex, pageSize, pageNo
     *                    等)，可为空
     * @return 接口响应 JSON
     */
    public String getOrderDetails(String startTime, String endTime, Map<String, Object> queryParams) {
        String url = BASE_URL + "/api/tb-service/get-order-details";

        Map<String, String> params = new TreeMap<>();
        params.put("startTime", startTime);
        params.put("endTime", endTime);
        params.put("appKey", appKey);
        params.put("version", "v1.0.0");

        if (queryParams != null && !queryParams.isEmpty()) {
            queryParams.forEach((k, v) -> {
                if (v != null) {
                    params.put(k, String.valueOf(v));
                }
            });
        }

        return doRequest(url, params);
    }

    /**
     * 获取商品详情
     *
     * @param goodsId 商品ID
     */
    public String getGoodsDetails(String goodsId) {
        String url = BASE_URL + "/open-api/goods/get_goods_detail_v2";

        Map<String, String> params = new TreeMap<>();
        params.put("goodsId", goodsId);
        params.put("appKey", appKey);
        params.put("version", "v1.0.0");

        return doRequest(url, params);
    }

    /**
     * 定时拉取商品
     *
     * @param pageId    页码
     * @param pageSize  每页数量
     * @param cid       分类ID (可选)
     * @param startTime 开始时间 yyyy-MM-dd HH:mm:ss
     * @param endTime   结束时间 yyyy-MM-dd HH:mm:ss
     */
    public String pullGoodsByTime(int pageId, int pageSize, String cid, String startTime, String endTime) {
        // 构建API URL
        String url = BASE_URL + "/api/goods/pull-goods-by-time";

        // 初始化参数Map
        Map<String, String> params = new TreeMap<>();
        // 设置页码参数
        params.put("pageId", String.valueOf(pageId));
        // 设置每页数量参数
        params.put("pageSize", String.valueOf(pageSize));
        // 如果cid不为空，设置cid参数
        if (cid != null && !cid.isEmpty()) {
            params.put("cid", cid);
        }
        // 如果startTime不为空，设置startTime参数
        if (startTime != null && !startTime.isEmpty()) {
            params.put("startTime", startTime);
        }
        // 如果endTime不为空，设置endTime参数
        if (endTime != null && !endTime.isEmpty()) {
            params.put("endTime", endTime);
        }
        // 设置appKey参数
        params.put("appKey", appKey);
        // 设置API版本
        params.put("version", "v1.2.3");

        // 发起请求并返回结果
        return doRequest(url, params);
    }

    /**
     * 获取失效商品
     *
     * @param pageId    页码
     * @param pageSize  每页数量
     * @param startTime 开始时间 yyyy-MM-dd HH:mm:ss
     * @param endTime   结束时间 yyyy-MM-dd HH:mm:ss
     */
    public String getStaleGoodsByTime(int pageId, int pageSize, String startTime, String endTime) {
        // 构建API URL
        String url = BASE_URL + "/api/goods/get-stale-goods-by-time";

        // 初始化参数Map
        Map<String, String> params = new TreeMap<>();
        // 设置页码参数
        params.put("pageId", String.valueOf(pageId));
        // 设置每页数量参数
        params.put("pageSize", String.valueOf(pageSize));
        // 如果startTime不为空，设置startTime参数
        if (startTime != null && !startTime.isEmpty()) {
            params.put("startTime", startTime);
        }
        // 如果endTime不为空，设置endTime参数
        if (endTime != null && !endTime.isEmpty()) {
            params.put("endTime", endTime);
        }
        // 设置appKey参数
        params.put("appKey", appKey);
        // 设置API版本
        params.put("version", "v1.0.1");

        // 发起请求并返回结果
        return doRequest(url, params);
    }

    /**
     * 获取更新商品
     *
     * @param pageId    页码
     * @param pageSize  每页数量
     * @param startTime 开始时间 yyyy-MM-dd HH:mm:ss
     * @param endTime   结束时间 yyyy-MM-dd HH:mm:ss
     */
    public String getNewestGoods(int pageId, int pageSize, String startTime, String endTime) {
        // 构建API URL
        String url = BASE_URL + "/api/goods/get-newest-goods";

        // 初始化参数Map
        Map<String, String> params = new TreeMap<>();
        // 设置页码参数
        params.put("pageId", String.valueOf(pageId));
        // 设置每页数量参数
        params.put("pageSize", String.valueOf(pageSize));
        // 如果startTime不为空，设置startTime参数
        if (startTime != null && !startTime.isEmpty()) {
            params.put("startTime", startTime);
        }
        // 如果endTime不为空，设置endTime参数
        if (endTime != null && !endTime.isEmpty()) {
            params.put("endTime", endTime);
        }
        // 设置appKey参数
        params.put("appKey", appKey);
        // 设置API版本
        params.put("version", "v1.2.0");

        // 发起请求并返回结果
        return doRequest(url, params);
    }

    /**
     * 高效转链
     *
     * @param goodsId    商品ID
     * @param pid        推广位ID (mm_xxx_xxx_xxx)
     * @param channelId  渠道ID (可选)
     * @param specialId  会员专属ID (可选)
     * @param externalId 外部ID (可选)
     */
    public String getPrivilegeLink(String goodsId, String pid, String channelId, String specialId, String externalId) {
        String url = BASE_URL + "/api/tb-service/get-privilege-link";

        Map<String, String> params = new TreeMap<>();
        params.put("version", "v1.3.1");
        params.put("appKey", appKey);
        params.put("goodsId", goodsId);

        String usePid = (pid != null && !pid.isBlank()) ? pid : defaultPid;
        if (usePid != null && !usePid.isBlank())
            params.put("pid", usePid);
        if (channelId != null)
            params.put("channelId", channelId);
        if (specialId != null)
            params.put("specialId", specialId);
        if (externalId != null)
            params.put("externalId", externalId);

        return doRequest(url, params);
    }

    /**
     * 执行API请求
     */
    private String doRequest(String url, Map<String, String> params) {
        try {
            // 新版验签参数
            String nonce = String.valueOf((int) ((Math.random() * 9 + 1) * 100000)); // 6位随机数
            String timer = String.valueOf(System.currentTimeMillis());

            params.put("nonce", nonce);
            params.put("timer", timer);

            // 生成签名
            String signRan = generateSign(nonce, timer);
            params.put("signRan", signRan);

            // 构建完整URL
            StringBuilder urlBuilder = new StringBuilder(url).append("?");
            // 参数排序不强制，但TreeMap本身有序
            params.forEach((key, value) -> urlBuilder.append(key).append("=").append(value).append("&"));
            String fullUrl = urlBuilder.substring(0, urlBuilder.length() - 1);

//            log.info("Requesting DTK API: {}", url);
            log.info("Requesting DTK Full URL: {}", fullUrl);

            // 发送请求
            String response = restTemplate.getForObject(fullUrl, String.class);
            log.info("DTK API Response: {}", response);

            return response;
        } catch (Exception e) {
            log.error("Failed to request DTK API: {}", url, e);
            return null;
        }
    }

    /**
     * 生成MD5签名 (新版)
     * 规则: appKey=xxx&timer=xxx&nonce=xxx&key=xxx -> MD5 -> HexUpper
     */
    private String generateSign(String nonce, String timer) {
        try {
            // 组装签名字符串
            String plainText = "appKey=" + appKey + "&timer=" + timer + "&nonce=" + nonce + "&key=" + appSecret;

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(plainText.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString().toUpperCase();
        } catch (Exception e) {
            log.error("Failed to generate sign", e);
            throw new RuntimeException("签名生成失败", e);
        }
    }

}
