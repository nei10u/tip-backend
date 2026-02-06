package com.nei10u.tip.scheduler.order.tb;

import com.nei10u.tip.ordersync.tb.TbOrderSyncService;
import com.nei10u.tip.ordersync.tb.TbSyncType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * TB 分钟级订单同步：
 * - 时间窗：最近 20 分钟
 * - 先同步 orderScene=2:渠道订单，再同步 orderScene=1:所有订单
 * - SyncType：MINUTE -> queryType=4
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.tb.scheduler", name = "enabled", havingValue = "true")
public class TbOrderMinScheduler {

    private final TbOrderSyncService tbOrderSyncService;

    // 每分钟第 35 秒
    @Scheduled(cron = "35 * * * * ?")
    public void scheduled() {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusMinutes(20);

        int c1 = tbOrderSyncService.syncRange(start, end, 2L, TbSyncType.MINUTE);
        int c2 = tbOrderSyncService.syncRange(start, end, 1L, TbSyncType.MINUTE);

        log.info("TB minute sync done: start={}, end={}, scene2={}, scene1={}", start, end, c1, c2);
    }
}


