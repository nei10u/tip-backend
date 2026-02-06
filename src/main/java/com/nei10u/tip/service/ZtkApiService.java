package com.nei10u.tip.service;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

/**
 * 折淘客API服务 (唯品会)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZtkApiService {

    private final RestTemplate restTemplate;

    @Value("${app.ztk.api-key:}")
    private String apiKey;

    @Value("${app.ztk.sid:}")
    private String sid;

    @Value("${app.ztk.tb-pid:}")
    private String tbPid;

    @Value("${app.jd.union-id}")
    private String unionId;

    private static final String BASE_URL_20000 = "http://api.zhetaoke.com:20000";

    private static final String BASE_URL_10001 = "https://api.zhetaoke.com:10001";

    // =========================
    // 抖音（Douyin）
    // =========================

    /**
     * 抖音活动列表API
     * 文档：open_douyin_activity_list.ashx
     *
     * @param pageId         页码（从 1 开始）
     * @param pageSize       每页数量（<=20）
     * @param activityStatus 活动状态：1待开始/2进行中/3已结束（不传默认进行中）
     */
    public String dyActivityList(int pageId, int pageSize, Integer activityStatus) {
        String url = BASE_URL_10001 + "/api/open_douyin_activity_list.ashx";
        Map<String, String> params = new TreeMap<>();
        params.put("appkey", apiKey);
        params.put("sid", sid);
        params.put("page", String.valueOf(Math.max(pageId, 1)));
        int ps = pageSize <= 0 ? 20 : Math.min(pageSize, 20);
        params.put("page_size", String.valueOf(ps));
        if (activityStatus != null && activityStatus > 0) {
            params.put("activity_status", String.valueOf(activityStatus));
        }
        return doRequest(url, params);
    }

    /**
     * 抖音活动转链API
     * 文档：open_douyin_zhuanlian_activity.ashx
     */
    public String dyActivityConvert(String materialId, String externalInfo, Boolean needQrCode) {
        String url = BASE_URL_10001 + "/api/open_douyin_zhuanlian_activity.ashx";
        Map<String, String> params = new TreeMap<>();
        params.put("appkey", apiKey);
        params.put("sid", sid);
        params.put("material_id", materialId == null ? "" : materialId.trim());
        if (StringUtils.isNotEmpty(externalInfo)) {
            params.put("external_info", externalInfo);
        }
        if (needQrCode != null) {
            params.put("need_qr_code", String.valueOf(needQrCode));
        }
        return doRequest(url, params);
    }

    /**
     * 抖音商品搜索API
     * 文档：open_douyin_product_search.ashx
     */
    public String dyProductSearch(String title, int pageId, int pageSize) {
        String url = BASE_URL_10001 + "/api/open_douyin_product_search.ashx";
        Map<String, String> params = new TreeMap<>();
        params.put("appkey", apiKey);
        params.put("sid", sid);
        if (StringUtils.isNotEmpty(title)) {
            params.put("title", title);
        }
        params.put("page", String.valueOf(Math.max(pageId, 1)));
        int ps = pageSize <= 0 ? 20 : Math.min(pageSize, 20);
        params.put("page_size", String.valueOf(ps));
        // 默认仅返回可分销商品（返利页更符合预期）
        params.put("share_status", "1");
        return doRequest(url, params);
    }

    /**
     * 抖音商品转链API
     * 文档：open_douyin_zhuanlian.ashx
     */
    public String dyProductConvert(String productUrl, String externalInfo, Boolean useCoupon, Boolean needShareLink, Boolean needQrCode) {
        String url = BASE_URL_10001 + "/api/open_douyin_zhuanlian.ashx";
        Map<String, String> params = new TreeMap<>();
        params.put("appkey", apiKey);
        params.put("sid", sid);
        params.put("product_url", productUrl == null ? "" : productUrl.trim());
        if (StringUtils.isNotEmpty(externalInfo)) {
            params.put("external_info", externalInfo);
        }
        if (useCoupon != null) {
            params.put("use_coupon", String.valueOf(useCoupon));
        }
        if (needShareLink != null) {
            params.put("need_share_link", String.valueOf(needShareLink));
        }
        if (needQrCode != null) {
            params.put("need_qr_code", String.valueOf(needQrCode));
        }
        return doRequest(url, params);
    }

    /**
     * 视频(抖货)商品API接口（折京客：api_videos.ashx）
     * <p>
     * 文档示例：
     * http://api.zhetaoke.com:20000/api/api_videos.ashx?appkey=#appkey#&page=1&page_size=20&sort=new
     * <p>
     * 说明：
     * - 该接口不支持 keyword/title 搜索（至少公开文档未提供），更多通过 sort/cid/sale_num_start 进行筛选。
     * - page_size 可配置 1-50。
     * - 若需要返回 total_count，可传 total_count=1（接口会返回总数而非列表，具体结构以折京客为准）。
     */
    public String dyVideoGoodsList(Integer cid,
                                   String saleNumStart,
                                   String sort,
                                   int pageId,
                                   int pageSize,
                                   Boolean totalCount) {
        String url = BASE_URL_20000 + "/api/api_videos.ashx";
        Map<String, String> params = new TreeMap<>();
        params.put("appkey", apiKey);

        params.put("page", String.valueOf(Math.max(pageId, 1)));

        int ps = pageSize <= 0 ? 20 : pageSize;
        if (ps > 50) ps = 50;
        params.put("page_size", String.valueOf(ps));

        params.put("sort", (sort == null || sort.isBlank()) ? "new" : sort.trim());

        if (cid != null && cid > 0) {
            params.put("cid", String.valueOf(cid));
        }
        if (saleNumStart != null && !saleNumStart.isBlank()) {
            params.put("sale_num_start", saleNumStart.trim());
        }
        if (totalCount != null && totalCount) {
            params.put("total_count", "1");
        }
        return doRequest(url, params);
    }

    /**
     * <a href="http://api.zhetaoke.com:20000/api/api_all.ashx?appkey=#appkey#&page=1&page_size=20&sort=new&cid=0&pinpai=1&pinpai_name=">...</a>
     * <p>
     * appkey	string	是	折京客的对接秘钥appkey
     * page	int	否	分页获取数据,第几页
     * page_size	int	否	每页数据条数（默认每页20条），可自定义1-50之间
     * <p>
     * sort	string	否	商品排序方式，new：按照综合排序，
     * total_sale_num_asc：按照总销量从小到大排序，total_sale_num_desc：按照总销量从大到小排序，
     * sale_num_asc：按照月销量从小到大排序，sale_num_desc：按照月销量从大到小排序，
     * commission_rate_asc：按照佣金比例从小到大排序，commission_rate_desc：按照佣金比例从大到小排序，
     * price_asc：按照价格从小到大排序，price_desc：按照价格从大到小排序，
     * coupon_info_money_asc：按照优惠券金额从小到大排序，coupon_info_money_desc：按照优惠券金额从大到小排序，
     * shop_level_asc：按照店铺等级从低到高排序，shop_level_desc：按照店铺等级从高到低排序，
     * tkfee_asc：按照返佣金额从低到高排序，tkfee_desc：按照返佣金额从高到低排序，
     * code：按照code值从大到小排序，date_time：按照更新时间排序，random：按照随机排序
     * <p>
     * cid	int	否	一级商品分类，值为空：全部商品，1：女装，2：母婴，3：美妆，4：居家日用，5：鞋品，6：美食，7：文娱车品，8：数码家电，9：男装，10：内衣，11：箱包，12：配饰，13：户外运动，14：家装家纺
     * pinpai	string	否	精选品牌，值为空：全部商品，1：精选品牌商品
     * pinpai_name	string	否	品牌名称，如：南极人、苏泊尔、美的等品牌。
     * tj string 否 值为空 全部商品，tmall：京东自营，gold_seller：京东好店
     */
    public String getJdGoodsList(String pinPaiName, String pinPai, String cid, String sort, String price, String tj,
                                 int pageId, int pageSize) {
        String url = BASE_URL_20000 + "/api/api_all.ashx";
        Map<String, String> params = new TreeMap<>();
        params.put("page", String.valueOf(pageId));
        params.put("page_size", String.valueOf(pageSize));
        params.put("appkey", apiKey);
        if (StringUtils.isNotEmpty(pinPaiName)) {
            params.put("pinpai_name", pinPaiName);
        }
        if (StringUtils.isNotEmpty(pinPai)) {
            params.put("pinpai", pinPai);
        }
        if (StringUtils.isNotEmpty(cid)) {
            params.put("cid", cid);
        }
        if (StringUtils.isNotEmpty(sort)) {
            params.put("sort", sort);
        }
        if (StringUtils.isNotEmpty(tj)) {
            params.put("tj", tj);
        }
        if (StringUtils.isNotEmpty(price)) {
            params.put("price", price);
        }
        return doRequest(url, params);
    }

    /**
     * 获取商品详情
     *
     * @param goodsId 商品ID
     */
    public String getJdGoodsDetails(String goodsId) {
        String url = BASE_URL_10001 + "/api/open_jd_union_open_goods_bigfield_query.ashx";

        Map<String, String> params = new TreeMap<>();
        params.put("appkey", apiKey);
        // 重要：content 必须进行 UrlEncode 编码（可传 skuId 或 jdUrl）
        // 统一在 doRequest 中对所有 query 参数做编码，避免重复编码/漏编码
        String content = goodsId == null ? "" : goodsId.trim();
        params.put("content", content);
        // 只取前端需要的字段：轮播图 + 详情图
        params.put("fields", "imageInfo");

        return doRequest(url, params);
    }

    /**
     * 获取唯品会商品列表
     */
    public String getVipGoodsList(String keyword, int pageId, int pageSize) {
        String url = BASE_URL_20000 + "/api/open/vip/goods/list";

        Map<String, String> params = new TreeMap<>();
        params.put("page", String.valueOf(pageId));
        params.put("page_size", String.valueOf(pageSize));
        params.put("appkey", apiKey);
        params.put("sid", sid);
        if (keyword != null) {
            params.put("keyword", keyword);
        }

        return doRequest(url, params);
    }

    /**
     * 实时人气榜（折淘客：api_shishi.ashx）
     * <p>
     * 实时返回人气榜单商品列表（前600个），返回佣金≥15%，动态描述分≥4.6的商品列表。
     * <p>
     * 文档：/api/api_shishi.ashx
     * 参数：
     * - appkey（必填）
     * - page（可选）
     * - page_size（可选 1-50）
     * - sort（可选，默认 new）
     * - cid（可选，一级分类）
     */
    public String getRealTimeHotList(String sort, int cid, int page, int pageSize) {
        String url = BASE_URL_20000 + "/api/api_shishi.ashx";

        Map<String, String> params = new TreeMap<>();
        params.put("appkey", apiKey);
        params.put("page", String.valueOf(Math.max(page, 1)));

        int ps = pageSize <= 0 ? 20 : pageSize;
        if (ps > 50) ps = 50;
        params.put("page_size", String.valueOf(ps));

        params.put("sort", (sort == null || sort.isBlank()) ? "new" : sort);
        if (cid > 0) {
            params.put("cid", String.valueOf(cid));
        }
        return doRequest(url, params);
    }

    /**
     * 京东转链API-新（自动匹配官方优惠券）
     *
     * @param materialId    string	是	推广物料url，例如活动链接、商品链接等；支持仅传入skuid
     *                      重要的事情说三遍：该参数需要进行Urlencode编码！该参数需要进行Urlencode编码！该参数需要进行Urlencode编码！
     * @param positionId    string	否	自定义推广位id
     *                      重要的事情说三遍：该参数可以自定义数字！该参数可以自定义数字！该参数可以自定义数字！
     *                      自定义的数字，自己在本地跟用户做好关联，订单中会透出自定义的数字。
     *                      如果返利需要用到此字段，如果是导购，不需要此字段。
     */
    public String jdLinkConvert(String materialId, String positionId) {
        String url = BASE_URL_10001 + "/api/open_jing_union_open_promotion_byunionid_get.ashx";
        Map<String, String> params = new TreeMap<>();
        params.put("appkey", apiKey);
        params.put("unionId", unionId);
        params.put("positionId", positionId);
        params.put("materialId", URLEncoder.encode(materialId, StandardCharsets.UTF_8));
        params.put("chainType", "3");
        return doRequest(url, params);
    }

    /**
     * 淘宝高佣转链（legacy: open_gaoyongzhuanlian.ashx）
     *
     * @param numIid     淘宝商品ID
     * @param relationId 渠道关系ID（归因关键）
     * @param pid        推广位PID（mm_xxx_xxx_xxx）；为空则使用 app.ztk.tb-pid，仍为空则不传
     */
    public String tbHighCommissionConvert(String numIid, String relationId, String pid) {
        String url = BASE_URL_20000 + "/api/open_gaoyongzhuanlian.ashx";

        Map<String, String> params = new TreeMap<>();
        params.put("appkey", apiKey);
        params.put("sid", sid);
        params.put("num_iid", numIid);
        if (relationId != null) {
            params.put("relation_id", relationId);
        }

        String usePid = (pid != null && !pid.isBlank()) ? pid : tbPid;
        if (usePid != null && !usePid.isBlank()) {
            params.put("pid", usePid);
        }

        // legacy 固定参数
        params.put("signurl", "5");

        return doRequest(url, params);
    }

    /**
     * 执行API请求
     */
    private String doRequest(String url, Map<String, String> params) {
        try {
            // 构建完整URL
            StringBuilder urlBuilder = new StringBuilder(url).append("?");
            params.forEach((key, value) -> {
                String v = value == null ? "" : value;
                // query 参数统一 UrlEncode（兼容中文/URL 等特殊字符）
                String enc = URLEncoder.encode(v, StandardCharsets.UTF_8);
                urlBuilder.append(key).append("=").append(enc).append("&");
            });
            String fullUrl = urlBuilder.substring(0, urlBuilder.length() - 1);

            log.info("Requesting ZTK API: {}", url);

            // 发送请求
            String response = restTemplate.getForObject(fullUrl, String.class);
            log.debug("ZTK API Response: {}", response);

            return response;
        } catch (Exception e) {
            log.error("Failed to request ZTK API: {}", url, e);
            return null;
        }
    }
}
