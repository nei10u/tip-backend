package com.nei10u.tip.service;

import com.alibaba.fastjson2.JSONObject;

/**
 * 活动服务（用于 /pages/jd 等活动入口）
 */
public interface ActivityService {

    /**
     * 京东：顶部 banner（取前 n 条）
     */
    JSONObject getJdTopBanners(int limit);

    /**
     * 京东：全部活动 banner（分页）
     */
    JSONObject getJdBanners(int pageId, int pageSize);

    /**
     * 抖音：顶部 banner（取前 n 条）
     */
    JSONObject getDyActivitiesBanners(int limit);

    /**
     * 抖音：活动列表（折淘客）
     */
    JSONObject getDyActivities(int pageId, int pageSize, Integer activityStatus);

    /**
     * 抖音：活动转链（折淘客）
     */
    JSONObject convertDyActivity(String materialId, String externalInfo, Boolean needQrCode);

}


