package com.nei10u.tip.service;

import com.nei10u.tip.vo.HomeConfigVO;

/**
 * 首页业务服务接口
 * <p>
 * 定义首页相关的业务逻辑方法。
 * 遵循面向接口编程的原则，将接口与实现分离。
 */
public interface HomeService {

    /**
     * 获取首页聚合配置
     * <p>
     * 该方法负责从数据库中查询首页所需的各类配置信息（轮播图、菜单、活动区），
     * 并将它们组装成一个 HomeConfigVO 对象返回。
     * 
     * @return HomeConfigVO 包含所有首页配置数据的视图对象
     */
    HomeConfigVO getHomeConfig();
}
