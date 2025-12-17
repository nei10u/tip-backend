package com.nei10u.tip.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nei10u.tip.model.HomeSection;
import org.apache.ibatis.annotations.Mapper;

/**
 * 首页活动专区数据库访问接口
 * <p>
 * 继承 MyBatis Plus 的 BaseMapper。
 * 如果需要复杂的自定义查询，可以在此接口中定义方法，并在对应的 XML 文件中编写 SQL。
 */
@Mapper
public interface HomeSectionMapper extends BaseMapper<HomeSection> {
}
