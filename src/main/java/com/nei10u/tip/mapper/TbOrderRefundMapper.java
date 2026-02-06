package com.nei10u.tip.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nei10u.tip.model.TbOrderRefund;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 淘宝退款证据链 Mapper。
 */
@Mapper
public interface TbOrderRefundMapper extends BaseMapper<TbOrderRefund> {

    /**
     * 按 tradeId 幂等 upsert（tradeId 唯一）。
     */
    int upsert(@Param("r") TbOrderRefund r);
}


