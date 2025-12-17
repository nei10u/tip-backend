package com.nei10u.tip.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nei10u.tip.model.WithdrawRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 提现记录Mapper
 */
@Mapper
public interface WithdrawMapper extends BaseMapper<WithdrawRecord> {

    /**
     * 查询用户的提现记录
     */
    List<WithdrawRecord> getRecordsByUserId(@Param("userId") Long userId);
}
