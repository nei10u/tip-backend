package com.nei10u.tip.scheduler.order.tb;

import com.nei10u.tip.ordersync.tb.TbRefundSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * TB 退款补偿（对齐 legacy RefundOrderSyncScheduler）：
 * - 每 30 分钟跑一次
 * - 从今天 00:00 往前 90 天，逐日调用 refund 接口
 * - 每天调用后 sleep 1s
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.tb.scheduler", name = "enabled", havingValue = "true")
public class TbRefundScheduler {

    private final TbRefundSyncService tbRefundSyncService;

    @Value("${app.tb.scheduler.refund-days:60}")
    private int refundDays;

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00:00");

    @Scheduled(cron = "0 0/30 * * * ?")
    public void scheduled() {
        int days = Math.max(1, refundDays);
        LocalDate start = LocalDate.now().minusDays(days);

        int total = 0;
        for (int i = 0; i <= days; i++) {
            String startTime = start.plusDays(i).format(DAY_FMT);
            total += tbRefundSyncService.syncByStartTime(startTime, 1L);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        log.info("TB refund sync done: days={}, totalItems={}", days, total);
    }
}


