package com.nei10u.tip.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.nei10u.tip.service.DtkApiService;
import com.nei10u.tip.service.TbConvertService;
import com.nei10u.tip.service.VeApiService;
import com.nei10u.tip.service.ZtkApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * TB 转链兜底链路（对齐 legacy TbGoodsData 的思路）：
 * ZTK(open_gaoyongzhuanlian) -> VEAPI(generalconvert) -> DTK(get-privilege-link)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TbConvertServiceImpl implements TbConvertService {

    private final ZtkApiService ztkApiService;
    private final VeApiService veApiService;
    private final DtkApiService dtkApiService;

    @Value("${app.dtk.pid:}")
    private String defaultPid;

    @Override
    public JSONObject convert(String goodsId, String relationId, String specialId, String pid, String externalId) {
        JSONObject out = new JSONObject();
        out.put("goodsId", goodsId);
        out.put("relationId", relationId);
        out.put("specialId", specialId);
        out.put("externalId", externalId);

        String usePid = StringUtils.hasText(pid) ? pid : defaultPid;

        // 1) ZTK 优先：只有在 relationId 存在时才尝试（归因稳定）
        if (StringUtils.hasText(relationId)) {
            JSONObject ztk = tryZtk(goodsId, relationId, usePid);
            if (ztk != null) {
                return merge(out, ztk);
            }
        }

        // 2) VEAPI 兜底：同样依赖 relationId（不然归因风险更高）
        if (StringUtils.hasText(relationId)) {
            JSONObject ve = tryVeApi(goodsId, relationId);
            if (ve != null) {
                return merge(out, ve);
            }
        }

        // 3) DTK 最后兜底：使用 specialId/externalId（若 relationId 缺失，仍可给出可用链接，但归因取决于上游回传字段）
        JSONObject dtk = tryDtk(goodsId, usePid, specialId, externalId);
        if (dtk != null) {
            if (!StringUtils.hasText(relationId)) {
                dtk.put("attributionWarning", "relationId为空：DTK 转链可能无法稳定回传 relation_id，归因可能依赖 special_id 或出现丢单风险");
            }
            return merge(out, dtk);
        }

        out.put("provider", "NONE");
        return out;
    }

    private static JSONObject merge(JSONObject base, JSONObject extra) {
        base.putAll(extra);
        return base;
    }

    private JSONObject tryZtk(String goodsId, String relationId, String pid) {
        try {
            String numIid = normalizeTbItemId(goodsId);
            if (!StringUtils.hasText(numIid)) return null;

            String resp = ztkApiService.tbHighCommissionConvert(numIid, relationId, pid);
            if (!StringUtils.hasText(resp)) return null;

            JSONObject json = JSON.parseObject(resp);
            Integer status = json.getInteger("status");
            if (status == null || status != 200) return null;

            JSONArray content = json.getJSONArray("content");
            if (content == null || content.isEmpty()) return null;

            JSONObject first = content.getJSONObject(0);
            String longUrl = first.getString("coupon_click_url");
            String shortUrl = first.getString("shorturl");
            String tkl = first.getString("tkl");

            if (!StringUtils.hasText(longUrl) && !StringUtils.hasText(shortUrl)) return null;

            JSONObject out = new JSONObject();
            out.put("provider", "ZTK");
            out.put("longUrl", longUrl);
            out.put("shortUrl", shortUrl);
            out.put("tpwd", normalizeTpwd(tkl));
            out.put("tpwdStr", normalizeTpwdStr(tkl));
            out.put("raw", json);
            return out;
        } catch (Exception e) {
            log.warn("ZTK convert failed: goodsId={}", goodsId, e);
            return null;
        }
    }

    private JSONObject tryVeApi(String goodsId, String relationId) {
        try {
            String resp = veApiService.generalConvert(goodsId, relationId);
            if (!StringUtils.hasText(resp)) return null;
            if (!resp.contains("\"error\":\"0\"")) return null; // legacy 约定

            JSONObject json = JSON.parseObject(resp);
            JSONObject data = json.getJSONObject("data");
            if (data == null) return null;

            // coupon 优先
            String longUrl = firstNonBlank(data,
                    "coupon_supered_long_url", "coupon_long_url",
                    "cps_supered_long_url", "cps_long_url");
            String shortUrl = firstNonBlank(data,
                    "coupon_supered_short_url", "coupon_short_url",
                    "cps_supered_short_url", "cps_short_url");
            String tpwd = firstNonBlank(data,
                    "coupon_supered_short_tpwd", "coupon_short_tpwd", "coupon_full_tpwd",
                    "cps_supered_short_tpwd", "cps_short_tpwd", "cps_full_tpwd");

            if (!StringUtils.hasText(longUrl) && !StringUtils.hasText(shortUrl)) return null;

            JSONObject out = new JSONObject();
            out.put("provider", "VEAPI");
            out.put("longUrl", longUrl);
            out.put("shortUrl", shortUrl);
            out.put("tpwd", normalizeTpwd(tpwd));
            out.put("tpwdStr", normalizeTpwdStr(tpwd));
            out.put("raw", json);
            return out;
        } catch (Exception e) {
            log.warn("VEAPI convert failed: goodsId={}", goodsId, e);
            return null;
        }
    }

    private JSONObject tryDtk(String goodsId, String pid, String specialId, String externalId) {
        try {
            String resp = dtkApiService.getPrivilegeLink(goodsId, pid, null, specialId, externalId);
            if (!StringUtils.hasText(resp)) return null;

            JSONObject json = JSON.parseObject(resp);
            JSONObject data = json.getJSONObject("data");
            if (data == null) return null;

            String longUrl = firstNonBlank(data, "couponClickUrl", "coupon_click_url", "longUrl", "long_url", "url");
            String shortUrl = firstNonBlank(data, "shortUrl", "short_url");
            String tpwd = firstNonBlank(data, "tpwd", "tPwd", "tkl", "taoWord");

            if (!StringUtils.hasText(longUrl) && !StringUtils.hasText(shortUrl)) return null;

            JSONObject out = new JSONObject();
            out.put("provider", "DTK");
            out.put("longUrl", longUrl);
            out.put("shortUrl", shortUrl);
            out.put("tpwd", normalizeTpwd(tpwd));
            out.put("tpwdStr", normalizeTpwdStr(tpwd));
            out.put("raw", json);
            return out;
        } catch (Exception e) {
            log.warn("DTK convert failed: goodsId={}", goodsId, e);
            return null;
        }
    }

    private static String normalizeTbItemId(String input) {
        if (!StringUtils.hasText(input)) return null;
        String s = input.trim();
        // legacy: 有些场景 itemId 形如 "xx-<num_iid>"
        if (s.contains("-")) {
            String[] parts = s.split("-");
            if (parts.length > 0) s = parts[parts.length - 1];
        }
        // 只保留数字
        if (s.matches("^\\d+$")) return s;
        return null;
    }

    private static String normalizeTpwd(String tkl) {
        if (!StringUtils.hasText(tkl)) return null;
        // legacy: 将 ￥ 替换为 /（避免前端展示/复制异常）
        return tkl.replace("￥", "/").trim();
    }

    private static String normalizeTpwdStr(String tkl) {
        if (!StringUtils.hasText(tkl)) return null;
        // legacy: 去掉 ￥
        return tkl.replace("￥", "").trim();
    }

    private static String firstNonBlank(JSONObject obj, String... keys) {
        if (obj == null || keys == null) return null;
        for (String k : keys) {
            String v = obj.getString(k);
            if (StringUtils.hasText(v)) return v;
        }
        return null;
    }
}



