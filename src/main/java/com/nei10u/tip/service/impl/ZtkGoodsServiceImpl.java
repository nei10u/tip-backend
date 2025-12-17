package com.nei10u.tip.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.nei10u.tip.service.ZtkApiService;
import com.nei10u.tip.service.ZtkGoodsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZtkGoodsServiceImpl implements ZtkGoodsService {

    private final ZtkApiService ztkApiService;

    @Override
    public JSONObject getVipGoodsList(int pageId, int pageSize, String keyword) {
        String response = ztkApiService.getVipGoodsList(pageId, pageSize, keyword);
        if (StringUtils.hasText(response)) {
            return JSON.parseObject(response);
        }
        return new JSONObject();
    }
}
