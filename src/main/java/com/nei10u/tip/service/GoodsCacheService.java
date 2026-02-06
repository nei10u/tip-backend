package com.nei10u.tip.service;

import com.alibaba.fastjson2.JSONObject;

public interface GoodsCacheService {

    JSONObject getOrLoadTbTrendFirstPage();

    JSONObject getOrLoadJdCurrentFirstPage();

    JSONObject fetchTbTrend(String type, String cid, int pageId, int pageSize);

    JSONObject fetchJdCurrentTrend(int cid, int pageId, int pageSize);
}
