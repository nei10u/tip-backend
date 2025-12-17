package com.nei10u.tip.ordersync.core;

import com.nei10u.tip.model.Order;
import com.nei10u.tip.ordersync.model.EcommercePlatform;
import com.nei10u.tip.ordersync.model.RawUnionOrder;
import com.nei10u.tip.ordersync.spi.EcommerceOrderMapper;
import com.nei10u.tip.ordersync.spi.UnionOrderSyncer;
import com.nei10u.tip.ordersync.support.ProfitCalculator;
import com.nei10u.tip.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 订单同步总协调器。
 * <p>
 * 该类作为订单同步的核心入口，负责编排整个同步流程。它采用了两层适配器模式来处理不同维度的数据差异：
 * <ol>
 * <li><strong>第一层（联盟平台维度）：</strong> 通过 {@link UnionOrderSyncer}
 * 从不同的联盟平台（如大淘客、高佣联盟）拉取标准化的原始数据。</li>
 * <li><strong>第二层（电商平台维度）：</strong> 通过 {@link EcommerceOrderMapper}
 * 将原始数据根据其所属的电商平台（淘宝、京东等）映射为系统内部统一的 {@link Order} 实体。</li>
 * </ol>
 * 最后，统一调用 {@link OrderService} 进行数据的持久化（Upsert）。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSyncCoordinator {

    // 注入所有注册的联盟平台同步器实现
    private final List<UnionOrderSyncer> unionOrderSyncers;
    // 注入所有注册的电商平台订单映射器实现
    private final List<EcommerceOrderMapper> ecommerceOrderMappers;
    private final ProfitCalculator profitCalculator;
    private final OrderService orderService;

    private static final ExecutorService ORDER_ASYNC_EXECUTOR =
            new ThreadPoolExecutor(8, 8, 0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(1024),
                    r -> {
                        Thread t = new Thread(r);
                        t.setName("order-async-worker");
                        return t;
                    }
            );

    /**
     * 同步指定时间范围内的所有订单。
     * 支持并行同步和详细日志。
     */
    public int syncRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (unionOrderSyncers == null || unionOrderSyncers.isEmpty()) {
            log.warn("No UnionOrderSyncer registered, skip sync. [未发现注册的联盟同步器，跳过同步]");
            return 0;
        }

        log.info("[Sync Start] Begin syncing orders from {} to {}. Total platforms: {}",
                startTime, endTime, unionOrderSyncers.size());

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        Map<EcommercePlatform, EcommerceOrderMapper> mapperMap = ecommerceOrderMappers.stream()
                .collect(Collectors.toMap(EcommerceOrderMapper::ecommercePlatform, m -> m, (a, b) -> a));

        AtomicInteger totalUpsert = new AtomicInteger(0);

        // 使用 CompletableFuture 并行执行每个联盟同步器
        List<CompletableFuture<Void>> futures = unionOrderSyncers.stream()
                .map(syncer -> CompletableFuture.runAsync(() -> {
                    String platformName = syncer.unionPlatform().name();
                    log.info("[Platform Start] {} sync started.", platformName);
                    long start = System.currentTimeMillis();
                    int platformUpsertCount = 0;

                    try {
                        // 使用流式处理，避免一次性加载所有订单
                        // 这里使用 AtomicInteger 是为了在 Lambda 内部计数
                        AtomicInteger batchCounter = new AtomicInteger(0);

                        syncer.processOrders(startTime, endTime, (batchRawOrders) -> {
                            if (batchRawOrders == null || batchRawOrders.isEmpty())
                                return;

                            List<Order> batchToUpsert = new ArrayList<>();
                            for (RawUnionOrder raw : batchRawOrders) {
                                EcommerceOrderMapper mapper = mapperMap.get(raw.getEcommercePlatform());
                                if (mapper == null) {
                                    log.warn("No mapper for ecommercePlatform={}, unionPlatform={}",
                                            raw.getEcommercePlatform(), raw.getUnionPlatform());
                                    continue;
                                }
                                try {
                                    Order order = mapper.mapToOrder(raw, profitCalculator);
                                    if (order != null) {
                                        batchToUpsert.add(order);
                                    }
                                } catch (Exception e) {
                                    log.error("Map order failed: ecommercePlatform={}, unionPlatform={}",
                                            raw.getEcommercePlatform(), raw.getUnionPlatform(), e);
                                }
                            }

                            // 分批次落库
                            if (!batchToUpsert.isEmpty()) {
                                int count = orderService.insertOrUpdateOrder(batchToUpsert);
                                batchCounter.addAndGet(count);
                                log.info("[Platform Progress] {} synced batch of {} orders.", platformName, count);
                            }
                        });

                        platformUpsertCount = batchCounter.get();
                        totalUpsert.addAndGet(platformUpsertCount);

                    } catch (Exception e) {
                        log.error("[Platform Failed] {} sync failed.", platformName, e);
                    } finally {
                        long cost = System.currentTimeMillis() - start;
                        log.info("[Platform Complete] {} sync finished. Upserted: {}. Cost: {}ms",
                                platformName, platformUpsertCount, cost);
                    }
                }, ORDER_ASYNC_EXECUTOR)).toList();

        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        stopWatch.stop();
        log.info("[Sync Summary] All platforms synced. Total upserted: {}. Total cost: {}ms",
                totalUpsert.get(), stopWatch.getTotalTimeMillis());

        return totalUpsert.get();
    }
}
