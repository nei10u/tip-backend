package com.nei10u.tip.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.nei10u.tip.service.JdConvertService;
import com.nei10u.tip.service.ZtkApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class JdConvertServiceImpl implements JdConvertService {

    private final ZtkApiService ztkApiService;

    /**
     * 京东转链API-新（自动匹配官方优惠券）
     *
     * @param materialId
     * @param positionId
     * @return
     */
    @Override
    public JSONObject convertLink(String materialId, String positionId) {
        String response = ztkApiService.jdLinkConvert(materialId, positionId);
        if (!StringUtils.hasText(response)) {
            return new JSONObject();
        }

        try {
            // ztk 外层返回结构示例：
            // {
            //   "jd_union_open_promotion_byunionid_get_response": {
            //     "code":"0",
            //     "result":"{\"code\":200,\"data\":{\"shortURL\":\"...\"},...}"
            //   }
            // }
            JSONObject outer = JSON.parseObject(response);
            JSONObject wrapper = outer.getJSONObject("jd_union_open_promotion_byunionid_get_response");
            if (wrapper == null) {
                // 兼容：如果上游已变更为直出 JSON（或 key 不一致），直接返回外层解析结果
                return outer;
            }

            // 优先解包 wrapper.result（可能是 JSON 字符串，也可能已经是对象）
            JSONObject inner = null;
            Object resultObj = wrapper.get("result");
            if (resultObj instanceof JSONObject) {
                inner = (JSONObject) resultObj;
            } else if (resultObj instanceof String resultStr) {
                if (StringUtils.hasText(resultStr)) {
                    try {
                        inner = JSON.parseObject(resultStr);
                    } catch (Exception parseEx) {
                        log.warn("JD link convert: failed to parse wrapper.result as json, result={}", resultStr);
                    }
                }
            }

            if (inner == null) {
                // 解包失败：至少把外层 wrapper 返回，方便前端/调用方看到 code/result 原文
                return wrapper;
            }

            // 对调用方友好：把 ztk 的 code 透出，同时把 data.shortURL 镜像到顶层 shortURL
            inner.putIfAbsent("ztkCode", wrapper.getString("code"));
            JSONObject data = inner.getJSONObject("data");
            if (data != null && StringUtils.hasText(data.getString("shortURL"))) {
                inner.putIfAbsent("shortURL", data.getString("shortURL"));
            }
            return inner;
        } catch (Exception e) {
            log.warn("JD link convert: failed to parse response, response={}", response, e);
            JSONObject fallback = new JSONObject();
            fallback.put("raw", response);
            return fallback;
        }
    }
}
