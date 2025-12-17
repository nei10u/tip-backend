package com.nei10u.tip.vo;

import com.nei10u.tip.model.HomeBanner;
import com.nei10u.tip.model.HomeMenu;
import com.nei10u.tip.model.HomeNotice;
import com.nei10u.tip.model.HomeSection;
import lombok.Data;
import java.util.List;

/**
 * 首页配置聚合视图对象
 */
@Data
public class HomeConfigVO {

    /**
     * 轮播图列表
     */
    private List<HomeBanner> banners;

    /**
     * 金刚区菜单列表
     */
    private List<HomeMenu> menus;

    /**
     * 活动专区列表
     */
    private List<HomeSection> sections;
    
    /**
     * 公告/跑马灯列表
     */
    private List<HomeNotice> notices;
}
