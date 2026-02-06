package com.nei10u.tip.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nei10u.tip.model.MoneyChange;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 资金流水 Mapper（用于审计与幂等）。
 */
@Mapper
public interface MoneyChangeMapper extends BaseMapper<MoneyChange> {

    /**
     * 幂等插入：若 uuid 已存在则不插入（返回 0）。
     */
    int insertIgnore(@Param("mc") MoneyChange mc);
}


