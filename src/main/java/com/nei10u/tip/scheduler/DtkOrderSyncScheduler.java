package com.nei10u.tip.scheduler;

import com.nei10u.tip.ordersync.core.OrderSyncCoordinator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 订单同步调度器。
 * <p>
 * 负责按预定的时间策略触发订单同步任务。
 * 采用了多级同步策略以确保数据的实时性和完整性（防漏单）。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DtkOrderSyncScheduler {

    private final OrderSyncCoordinator orderSyncCoordinator;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 高频同步任务（分钟级）。
     * <p>
     * 策略：每 15 分钟触发一次，同步过去 15 分钟内的订单。
     * 目的：主要负责近实时数据的抓取，因各联盟平台数据可能有分钟级延迟，
     * 这个窗口可以覆盖大部分新产生的订单。
     * </p>
     */
//    @Scheduled(cron = "0 */15 * * * ?")
    public void syncOrdersMinutely() {
        log.info("开始执行分钟级订单同步...");

        LocalDateTime endTime = LocalDateTime.now();
        // 向前推15分钟
        LocalDateTime startTime = endTime.minusMinutes(15);

        syncOrders(startTime, endTime);
    }

    /**
     * 每日兜底同步任务（天级）。
     * <p>
     * 策略：每天凌晨 01:00 触发，同步前一整天（00:00:00 - 23:59:59）的订单。
     * 目的：
     * 1. 修复因网络波动、接口超时等原因导致的高频同步漏单。
     * 2. 同步订单状态的变更（如订单失效、维权、结算等，这些状态可能在下单后很久才更新）。
     * </p>
     */
//    @Scheduled(cron = "0 0 1 * * ?")
    public void syncOrdersDaily() {
        log.info("开始执行天级订单同步...");

        // 今天的0点即为昨天的结束
        LocalDateTime endTime = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime startTime = endTime.minusDays(1);

        syncOrders(startTime, endTime);
    }

    /**
     * 每月历史回溯同步任务（月级）。
     * <p>
     * 策略：每月1号凌晨 02:00 触发，同步上个月的订单。
     * 目的：主要确保月度结算数据的准确性，处理跨月结算或长周期的维权订单。
     * </p>
     */
//    @Scheduled(cron = "0 0 2 1 * ?")
    public void syncOrdersMonthly() {
        log.info("开始执行月度订单同步...");

        // 本月1号0点
        LocalDateTime endTime = LocalDateTime.now().withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0);
        // 上月1号0点
        LocalDateTime startTime = endTime.minusMonths(1);

        syncOrders(startTime, endTime);
    }

    /**
     * 执行同步的通用方法。
     * 捕获所有异常，确保调度任务不会因此崩溃。
     */
    private void syncOrders(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            log.info("同步订单时间范围: {} - {}", startTime.format(FORMATTER), endTime.format(FORMATTER));
            int count = orderSyncCoordinator.syncRange(startTime, endTime);
            log.info("订单同步完成，upsert count={}", count);

        } catch (Exception e) {
            log.error("订单同步失败", e);
        }
    }
}
