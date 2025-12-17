package com.nei10u.tip.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nei10u.tip.model.HomeBanner;
import org.apache.ibatis.annotations.Mapper;

/**
 * 首页轮播图数据库访问接口
 * <p>
 * 继承 MyBatis Plus 的 BaseMapper，自动拥有 CRUD 能力。
 * 无需编写 XML 文件即可完成基本的增删改查。
 * 
 * @Mapper 注解由 MyBatis 提供，用于标识这是一个 Mapper 接口，Spring 启动时会自动扫描并创建代理对象。
 */
@Mapper
public interface HomeBannerMapper extends BaseMapper<HomeBanner> {
}
