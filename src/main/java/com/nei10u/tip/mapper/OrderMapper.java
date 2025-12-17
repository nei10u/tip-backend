package com.nei10u.tip.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nei10u.tip.model.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单Mapper (基于参考实现)
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 根据用户ID分页查询订单
     */
    IPage<Order> getOrdersByUserId(Page<?> page, @Param("sid") String sid);

    /**
     * 根据用户ID和状态查询订单
     */
    IPage<Order> getOrdersByStatus(Page<?> page,
            @Param("sid") String sid,
            @Param("statusList") List<Byte> statusList);

    /**
     * 批量插入或更新订单
     */
    int insertOrUpdateBatch(@Param("orders") List<Order> orders);

    /**
     * 根据订单号查询
     */
    Order getOrderByOrderSn(@Param("orderSn") String orderSn);

    /**
     * 统计订单数量
     */
    Integer countOrdersByStatus(@Param("sid") String sid, @Param("statusList") List<Byte> statusList);

    /**
     * 统计预估佣金
     */
    Double sumShareFeeByStatus(@Param("sid") String sid, @Param("statusList") List<Byte> statusList);
}
