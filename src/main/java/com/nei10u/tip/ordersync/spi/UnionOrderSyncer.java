package com.nei10u.tip.ordersync.spi;

import com.nei10u.tip.ordersync.model.RawUnionOrder;
import com.nei10u.tip.ordersync.model.UnionPlatform;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

/**
 * 联盟平台订单同步器（第一层多态）。
 * <p>
 * 该接口定义了从不同联盟平台（如大淘客、好单库等）拉取原始订单数据的标准规范。
 * 实现类负责屏蔽不同联盟平台的 API 差异，统一返回 {@link RawUnionOrder} 格式的数据。
 * </p>
 *
 * <h3>职责说明：</h3>
 * <ul>
 * <li>调用指定联盟平台的 API 拉取订单数据（处理分页、签名等细节）。</li>
 * <li>将联盟平台返回的原始 JSON 数据拆解为 {@link RawUnionOrder} 列表。</li>
 * <li>初步识别订单所属的电商平台（如淘宝、京东、拼多多），并设置到。</li>
 * </ul>
 *
 * <h3>非职责范围：</h3>
 * <ul>
 * <li>不负责将订单数据映射为本系统的 {@code Order} 实体（该工作由第二层多态 {@code EcommerceOrderMapper}
 * 完成）。</li>
 * <li>不负责订单数据的持久化（落库）。</li>
 * </ul>
 */
public interface UnionOrderSyncer {

    /**
     * 获取当前同步器所属的联盟平台标识。
     *
     * @return 联盟平台枚举值
     */
    UnionPlatform unionPlatform();

    /**
     * 流式处理订单数据（核心接口）。
     * <p>
     * 分批拉取并处理订单，适用于大数据量同步场景。
     * 实现类必须实现此方法，通过回调 consumer 逐批返回数据，避免内存溢出。
     * </p>
     *
     * @param startTime     开始时间
     * @param endTime       结束时间
     * @param batchConsumer 用于消费每一批次订单的 Consumer
     */
    void processOrders(LocalDateTime startTime, LocalDateTime endTime,
            Consumer<List<RawUnionOrder>> batchConsumer);

}
