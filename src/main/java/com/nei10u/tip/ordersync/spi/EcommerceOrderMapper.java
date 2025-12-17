package com.nei10u.tip.ordersync.spi;

import com.nei10u.tip.model.Order;
import com.nei10u.tip.ordersync.model.EcommercePlatform;
import com.nei10u.tip.ordersync.model.RawUnionOrder;
import com.nei10u.tip.ordersync.support.ProfitCalculator;

/**
 * 电商平台订单映射器（第二层多态）。
 *
 * 负责：
 * - 将 RawUnionOrder（联盟平台原始订单）解析并映射为本站 Order
 * - 在映射过程中调用 ProfitCalculator 计算“本站盈利/用户可得展示口径”
 */
public interface EcommerceOrderMapper {

    EcommercePlatform ecommercePlatform();

    Order mapToOrder(RawUnionOrder rawOrder, ProfitCalculator profitCalculator);
}

