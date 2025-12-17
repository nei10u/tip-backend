package com.nei10u.tip.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nei10u.tip.model.HomeMenu;
import org.apache.ibatis.annotations.Mapper;

/**
 * 首页菜单数据库访问接口
 * <p>
 * 继承 MyBatis Plus 的 BaseMapper，获得标准的数据访问能力。
 * 泛型 HomeMenu 指定了该 Mapper 操作的实体类型。
 */
@Mapper
public interface HomeMenuMapper extends BaseMapper<HomeMenu> {
}
