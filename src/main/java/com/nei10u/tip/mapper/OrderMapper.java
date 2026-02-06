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
    IPage<Order> getOrdersByUserId(Page<?> page, @Param("userId") Long userId);

    /**
     * 根据用户ID和状态查询订单
     */
    IPage<Order> getOrdersByStatus(Page<?> page,
            @Param("userId") Long userId,
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
    Integer countOrdersByStatus(@Param("userId") Long userId, @Param("statusList") List<Byte> statusList);

    /**
     * 统计预估佣金
     */
    Double sumShareFeeByStatus(@Param("userId") Long userId, @Param("statusList") List<Byte> statusList);

    /**
     * 按 payMonth 汇总预计可入账金额（排除失效/锁单/维权中）。
     */
    Double sumReceivableByPayMonth(@Param("userId") Long userId, @Param("payMonth") String payMonth);

    /**
     * 汇总 payMonth 大于指定 key 的预计可入账金额（排除失效/锁单/维权中）。
     */
    Double sumToBeReceiveAfterPayMonth(@Param("userId") Long userId, @Param("payMonth") String payMonth);

    /**
     * 汇总已入账金额（订单维度）。
     */
    Double sumCreditedFee(@Param("userId") Long userId);

    /**
     * 回填历史订单 user_id（通过 orders.sid 匹配 users.relation_id/special_id/pdd_pid/union_id）
     */
    int backFillOrderUserId();
}
