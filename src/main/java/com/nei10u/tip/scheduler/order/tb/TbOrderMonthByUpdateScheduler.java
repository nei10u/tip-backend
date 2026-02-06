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
 * TB 月补偿（按更新时间维度 MONTH_UPDATE -> queryType=4）。
 * 未绑定淘宝账户关系或者系统没有识别到关系，但是算作我们渠道的订单。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.tb.scheduler", name = "enabled", havingValue = "true")
public class TbOrderMonthByUpdateScheduler {

    private final TbOrderSyncService tbOrderSyncService;

    @Value("${app.tb.scheduler.back-fill-days:365}")
    private int backFillDays;

    @Value("${app.tb.scheduler.slice-minutes:20}")
    private int sliceMinutes;

    // 每月 22 日 04:30:55
    @Scheduled(cron = "55 30 4 22 * ?")
    public void scheduled() {
        int days = Math.max(1, backFillDays);
        int step = Math.max(1, sliceMinutes);

        LocalDateTime end = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);
        LocalDateTime start = end.minusDays(days);

        long slices = (long) days * 24 * (60 / step);
        log.info("TB month(update) sync start: days={}, stepMinutes={}, slices={}", days, step, slices);

        LocalDateTime cursor = start;
        for (long i = 0; i < slices; i++) {
            LocalDateTime next = cursor.plusMinutes(step);
            tbOrderSyncService.syncRange(cursor, next, 2L, TbSyncType.MONTH_UPDATE);
            cursor = next;
        }

        log.info("TB month(update) sync done");
    }
}


