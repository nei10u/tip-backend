package com.nei10u.tip.service;

import com.alibaba.fastjson2.JSONObject;

public interface CmsRebateConfigCacheService {
    /**
     * 获取某个平台的“已发布返利页配置”（tip-backend 缓存）。
     * 缓存缺失时会尝试从 tip-cms 拉取一次并写入缓存。
     */
    JSONObject getPublishedPage(String platformCode);

    /**
     * 强制从 tip-cms 拉取并刷新某个平台的配置缓存。
     */
    JSONObject refreshPublishedPage(String platformCode);

    /**
     * 批量刷新（用于发布动作）。
     */
    void refreshAll();
}

