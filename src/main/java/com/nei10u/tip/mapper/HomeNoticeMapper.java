package com.nei10u.tip.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nei10u.tip.model.HomeNotice;
import org.apache.ibatis.annotations.Mapper;

/**
 * 首页公告数据库访问接口
 */
@Mapper
public interface HomeNoticeMapper extends BaseMapper<HomeNotice> {
}

