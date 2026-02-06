package com.nei10u.tip.ordersync.tb;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Map;

/**
 * TB OpenAPI 统一调用入口（避免在各处散落 callTb/callRefund/callPunish）。
 *
 * 职责：
 * - 统一执行：method + params -> HTTP -> JSON parse
 * - 统一识别 error_response 并记录日志（避免上层重复写）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TbOpenApiService {

    private final TbOpenAPIClient tbOpenAPIClient;

    public JSONObject executeJson(String method, Map<String, String> params, String bizTag) {
        try {
            Map<String, String> p = (params == null) ? Collections.emptyMap() : params;
            String raw = tbOpenAPIClient.execute(method, p);
            if (!StringUtils.hasText(raw)) return null;

            JSONObject parsed = JSON.parseObject(raw);
            JSONObject err = parsed.getJSONObject("error_response");
            if (err != null) {
                log.warn("TB OpenAPI error [{}]: method={}, code={}, subCode={}, msg={}, subMsg={}",
                        (bizTag == null ? "-" : bizTag),
                        method,
                        err.getString("code"),
                        err.getString("sub_code"),
                        err.getString("msg"),
                        err.getString("sub_msg"));
                return null;
            }
            return parsed;
        } catch (Exception e) {
            log.error("TB OpenAPI call failed [{}]: method={}", (bizTag == null ? "-" : bizTag), method, e);
            return null;
        }
    }

    /** taobao.tbk.order.details.get */
    public JSONObject orderDetailsGet(Map<String, String> params) {
        return executeJson("taobao.tbk.order.details.get", params, "orderDetails");
    }

    /** taobao.tbk.relation.refund */
    public JSONObject relationRefund(Map<String, String> params) {
        return executeJson("taobao.tbk.relation.refund", params, "refund");
    }

    /** taobao.tbk.sc.punish.order.get（权限可能受限） */
    public JSONObject punishOrderGet(Map<String, String> params) {
        return executeJson("taobao.tbk.sc.punish.order.get", params, "punish");
    }
}


