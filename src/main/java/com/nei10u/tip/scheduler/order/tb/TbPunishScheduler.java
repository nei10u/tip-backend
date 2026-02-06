package com.nei10u.tip.scheduler.order.tb;

import com.nei10u.tip.ordersync.tb.TbPunishSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * TB 处罚/违规补偿（对齐 PunishOrderSyncScheduler 的“逐日扫 + sleep”节奏）：
 * - 每 3 小时一次
 * - 从今天 00:00 往前 90 天，逐日调用 punish 接口
 * - 每天 sleep 500ms
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.tb.scheduler", name = "enabled", havingValue = "true")
public class TbPunishScheduler {

    private final TbPunishSyncService tbPunishSyncService;

    @Value("${app.tb.scheduler.back-fill-days:90}")
    private int backFillDays;

    @Value("${app.tb.scheduler.punish-page-size:100}")
    private int pageSize;

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00:00");

    @Scheduled(cron = "0 0 0/3 * * ?")
    public void scheduled() {
        int days = Math.max(1, backFillDays);
        int size = Math.max(1, pageSize);

        LocalDate start = LocalDate.now().minusDays(days);
        int total = 0;

        for (int i = 1; i < days; i++) {
            String startTime = start.plusDays(i - 1).format(DAY_FMT);
            total += tbPunishSyncService.syncByStartTime(startTime, 1, size);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        log.info("TB punish sync done: days={}, totalItems={}", days, total);
    }
}


