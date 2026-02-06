package com.nei10u.tip.scheduler.order.tb;

import com.nei10u.tip.ordersync.tb.TbOrderSyncService;
import com.nei10u.tip.ordersync.tb.TbSyncType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * TB 日补偿订单同步（对齐 legacy OrderSyncTBDayScheduler）：
 * - 以“今天 00:00:00”为基准，向前回溯 90 天
 * - 按 20 分钟切片循环（6480 次 = 90*24*3）
 * - 每个切片跑两条：筛选订单类型，1:所有订单，2:渠道订单，3:会员运营订单，默认为1
 * - SyncType：DAY -> queryType=4
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.tb.scheduler", name = "enabled", havingValue = "true")
public class TbOrderDayScheduler {

    private final TbOrderSyncService tbOrderSyncService;

    @Value("${app.tb.scheduler.back-fill-days:90}")
    private int backFillDays;

    @Value("${app.tb.scheduler.slice-minutes:20}")
    private int sliceMinutes;

    // 每天 01:00:00
    @Scheduled(cron = "0 0 1 * * ?")
    public void scheduled() {
        int days = Math.max(1, backFillDays);
        int step = Math.max(1, sliceMinutes);

        LocalDateTime end = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);
        LocalDateTime start = end.minusDays(days);

        long slices = (long) days * 24 * (60 / step);
        log.info("TB day sync start: days={}, stepMinutes={}, slices={}", days, step, slices);

        LocalDateTime cursor = start;
        for (long i = 0; i < slices; i++) {
            LocalDateTime next = cursor.plusMinutes(step);
            tbOrderSyncService.syncRange(cursor, next, 2L, TbSyncType.DAY);
            tbOrderSyncService.syncRange(cursor, next, 1L, TbSyncType.DAY);
            cursor = next;
        }

        log.info("TB day sync done");
    }
}


