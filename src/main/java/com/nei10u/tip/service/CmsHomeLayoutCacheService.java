package com.nei10u.tip.service;

import com.nei10u.tip.vo.HomeLayoutConfigVO;

public interface CmsHomeLayoutCacheService {
    /**
     * 获取当前缓存；若缓存为空会尝试从数据库或 tip-cms 拉取并写入缓存。
     */
    HomeLayoutConfigVO getCachedOrRefresh();

    /**
     * 强制从 tip-cms 拉取并刷新缓存。
     */
    HomeLayoutConfigVO refreshFromCms();

    /**
     * 强制从数据库读取并刷新缓存（cms.home_page_section）。
     */
    HomeLayoutConfigVO refreshFromDb();
}

