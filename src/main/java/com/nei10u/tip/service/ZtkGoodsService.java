package com.nei10u.tip.service;

import com.alibaba.fastjson2.JSONObject;

public interface ZtkGoodsService {

    /**
     * 获取唯品会商品列表
     */
    JSONObject getVipGoodsList(int pageId, int pageSize, String keyword);
}
