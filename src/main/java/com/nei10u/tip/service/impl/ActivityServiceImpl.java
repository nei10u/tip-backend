package com.nei10u.tip.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nei10u.tip.goods.util.GoodsFieldUtils;
import com.nei10u.tip.mapper.DsConfigActivityMapper;
import com.nei10u.tip.model.DsConfigActivity;
import com.nei10u.tip.service.ActivityService;
import com.nei10u.tip.service.ZtkApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final DsConfigActivityMapper dsConfigActivityMapper;
    private final ZtkApiService ztkApiService;

    /**
     * 当前 tip-backend 尚未沉淀“活动平台/分类聚合”的完整逻辑；
     * 为满足 /pages/jd 的“banner 活动列表”，先按 legacy 的 ActivityBean 逻辑：
     * - status = 1
     * - support_banner = true
     * - support_app = true（APP 端展示）
     *
     * 后续如需要按 ds_activity_category.type=jd 精准过滤，再补充关联字段/映射。
     */
    private QueryWrapper<DsConfigActivity> jdBannerBaseQw() {
        QueryWrapper<DsConfigActivity> qw = new QueryWrapper<>();
        qw.eq("status", 1);
        qw.eq("support_banner", true);
        qw.eq("support_app", true);
        // 优先按 scale（若运营用它控制顺序）；再按 act_id
        qw.orderByDesc("scale").orderByAsc("act_id");
        return qw;
    }

    @Override
    public JSONObject getJdTopBanners(int limit) {
        int n = limit <= 0 ? 2 : Math.min(limit, 20);
        List<DsConfigActivity> list = dsConfigActivityMapper.selectList(jdBannerBaseQw().last("limit " + n));

        JSONObject out = new JSONObject();
        out.put("list", toBannerPayload(list));
        out.put("totalNum", list.size());
        out.put("pageId", 1);
        out.put("pageSize", n);
        return out;
    }

    @Override
    public JSONObject getJdBanners(int pageId, int pageSize) {
        int p = Math.max(pageId, 1);
        int ps = pageSize <= 0 ? 20 : Math.min(pageSize, 50);

        Page<DsConfigActivity> page = new Page<>(p, ps);
        Page<DsConfigActivity> res = dsConfigActivityMapper.selectPage(page, jdBannerBaseQw());

        JSONObject out = new JSONObject();
        out.put("list", toBannerPayload(res.getRecords()));
        out.put("totalNum", res.getTotal());
        out.put("pageId", String.valueOf(p));
        out.put("pageSize", ps);
        return out;
    }

    @Override
    public JSONObject getDyActivitiesBanners(int limit) {
        String resp = ztkApiService.dyActivityList(1, 2, 2);
        JSONObject out = new JSONObject();
        out.put("pageId", String.valueOf(1));
        out.put("pageSize", 2);

        if (!StringUtils.hasText(resp)) {
            out.put("list", new JSONArray());
            out.put("totalNum", 0);
            out.put("raw", new JSONObject());
            return out;
        }

        JSONObject raw;
        try {
            raw = JSON.parseObject(resp);
        } catch (Exception e) {
            out.put("list", new JSONArray());
            out.put("totalNum", 0);
            out.put("raw", resp);
            return out;
        }

        JSONObject data = raw.getJSONObject("data");
        JSONArray arr = data == null ? null : data.getJSONArray("activity_material_list");
        if (arr == null) arr = new JSONArray();

        JSONArray list = new JSONArray();
        for (int i = 0; i < arr.size(); i++) {
            JSONObject it = arr.getJSONObject(i);
            if (it == null) continue;

            JSONObject one = new JSONObject();
            one.put("platform", "dy");
            one.put("materialId", it.getString("material_id"));
            one.put("activityId", it.getString("activity_id"));

            one.put("title", GoodsFieldUtils.firstNonBlank(it, "activity_name", "title"));
            one.put("banner", GoodsFieldUtils.firstNonBlank(it,
                    "activity_promotion_cover",
                    "activity_main_cover",
                    "activity_preview_cover"));
            // 作为落地页 H5：优先 promotion asset，其次 rule link
            one.put("h5Url", GoodsFieldUtils.firstNonBlank(it,
                    "activity_promotion_asset",
                    "activity_rule_link"));

            one.put("startTime", it.getString("start_time"));
            one.put("endTime", it.getString("end_time"));
            list.add(one);
        }

        Long total = data == null ? null : data.getLong("total");
        out.put("list", list);
        out.put("totalNum", (total != null && total > 0) ? total : list.size());
        out.put("raw", raw);
        return out;
    }

    @Override
    public JSONObject getDyActivities(int pageId, int pageSize, Integer activityStatus) {
        int p = Math.max(pageId, 1);
        int ps = pageSize <= 0 ? 20 : Math.min(pageSize, 20);

        String resp = ztkApiService.dyActivityList(p, ps, activityStatus);
        JSONObject out = new JSONObject();
        out.put("pageId", String.valueOf(p));
        out.put("pageSize", ps);

        if (!StringUtils.hasText(resp)) {
            out.put("list", new JSONArray());
            out.put("totalNum", 0);
            out.put("raw", new JSONObject());
            return out;
        }

        JSONObject raw;
        try {
            raw = JSON.parseObject(resp);
        } catch (Exception e) {
            out.put("list", new JSONArray());
            out.put("totalNum", 0);
            out.put("raw", resp);
            return out;
        }

        JSONObject data = raw.getJSONObject("data");
        JSONArray arr = data == null ? null : data.getJSONArray("activity_material_list");
        if (arr == null) arr = new JSONArray();

        JSONArray list = new JSONArray();
        for (int i = 0; i < arr.size(); i++) {
            JSONObject it = arr.getJSONObject(i);
            if (it == null) continue;

            JSONObject one = new JSONObject();
            one.put("platform", "dy");
            one.put("materialId", it.getString("material_id"));
            one.put("activityId", it.getString("activity_id"));

            one.put("title", GoodsFieldUtils.firstNonBlank(it, "activity_name", "title"));
            one.put("banner", GoodsFieldUtils.firstNonBlank(it,
                    "activity_promotion_cover",
                    "activity_main_cover",
                    "activity_preview_cover"));
            // 作为落地页 H5：优先 promotion asset，其次 rule link
            one.put("h5Url", GoodsFieldUtils.firstNonBlank(it,
                    "activity_promotion_asset",
                    "activity_rule_link"));

            one.put("startTime", it.getString("start_time"));
            one.put("endTime", it.getString("end_time"));
            list.add(one);
        }

        Long total = data == null ? null : data.getLong("total");
        out.put("list", list);
        out.put("totalNum", (total != null && total > 0) ? total : list.size());
        out.put("raw", raw);
        return out;
    }

    @Override
    public JSONObject convertDyActivity(String materialId, String externalInfo, Boolean needQrCode) {
        String resp = ztkApiService.dyActivityConvert(materialId, externalInfo, needQrCode);
        if (!StringUtils.hasText(resp)) return new JSONObject();

        JSONObject raw;
        try {
            raw = JSON.parseObject(resp);
        } catch (Exception e) {
            return new JSONObject().fluentPut("raw", resp);
        }

        JSONObject data = raw.getJSONObject("data");
        JSONObject inner = data == null ? null : data.getJSONObject("data");
        if (inner == null) inner = data;
        if (inner == null) inner = raw;

        // 统一映射：click_url + tpwd，供 App 通用解析
        String clickUrl = GoodsFieldUtils.firstNonBlank(inner, "dy_zlink", "share_link");
        String tpwd = GoodsFieldUtils.firstNonBlank(inner, "dy_password");
        JSONObject out = new JSONObject();
        if (StringUtils.hasText(clickUrl)) out.put("click_url", clickUrl);
        if (StringUtils.hasText(tpwd)) out.put("tpwd", tpwd);
        out.put("raw", raw);
        return out;
    }

    /**
     * 前端期望字段（你给的示例）：
     * - clickURL / shortURL / jdAppUrl（或 appUrl）
     *
     * 存储约定（自动识别）：
     * - rule: JSON 文本，优先读取 clickURL/shortURL/appUrl/jdAppUrl
     * - path: 若是 url，则作为 clickURL 兜底
     */
    private JSONArray toBannerPayload(List<DsConfigActivity> rows) {
        JSONArray arr = new JSONArray();
        if (rows == null) return arr;

        for (DsConfigActivity a : rows) {
            JSONObject one = new JSONObject();
            one.put("id", a.getId());
            one.put("actId", a.getActId());
            one.put("title", a.getDisplayName());
            one.put("banner", a.getBanner());
            one.put("jumpType", a.getJumpType());

            String clickURL = "";
            String shortURL = "";
            String appUrl = "";

            // 1) rule JSON 优先
            String rule = a.getRule();
            if (rule != null && !rule.isBlank()) {
                try {
                    JSONObject r = JSON.parseObject(rule);
                    if (r != null) {
                        clickURL = firstNonBlank(r, "clickURL", "clickUrl", "click_url", "url");
                        shortURL = firstNonBlank(r, "shortURL", "shortUrl", "short_url");
                        appUrl = firstNonBlank(r, "jdAppUrl", "appUrl", "app_url", "openAppUrl");
                    }
                } catch (Exception e) {
                    log.debug("rule json parse failed, actId={}", a.getActId(), e);
                }
            }

            // 2) path 兜底
            String path = a.getPath();
            if ((clickURL == null || clickURL.isBlank()) && path != null && path.startsWith("http")) {
                clickURL = path;
            }

            one.put("clickURL", clickURL);
            one.put("shortURL", shortURL);
            one.put("jdAppUrl", appUrl);
            arr.add(one);
        }
        return arr;
    }

    private static String firstNonBlank(JSONObject jo, String... keys) {
        if (jo == null || keys == null) return "";
        for (String k : keys) {
            Object v = jo.get(k);
            if (v == null) continue;
            String s = v.toString().trim();
            if (!s.isEmpty() && !"null".equalsIgnoreCase(s)) return s;
        }
        return "";
    }

}


