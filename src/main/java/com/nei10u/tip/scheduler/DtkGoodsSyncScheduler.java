package com.nei10u.tip.scheduler;

import com.nei10u.tip.service.DtkGoodsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 商品同步调度器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DtkGoodsSyncScheduler {

    // 注入商品服务
    private final DtkGoodsService goodsService;

    /**
     * 每5分钟同步一次商品
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void syncGoods() {
        // 记录任务开始日志
        log.info("开始执行商品同步任务...");
        try {
            // 调用商品同步服务
            goodsService.syncGoods();
            // 记录任务完成日志
            log.info("商品同步任务完成");
        } catch (Exception e) {
            // 记录任务异常日志
            log.error("商品同步任务异常", e);
        }
    }

    /**
     * 每5分钟同步一次失效商品
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void syncStaleGoods() {
        // 记录任务开始日志
        log.info("开始执行失效商品同步任务...");
        try {
            // 调用失效商品同步服务
            goodsService.syncStaleGoods();
            // 记录任务完成日志
            log.info("失效商品同步任务完成");
        } catch (Exception e) {
            // 记录任务异常日志
            log.error("失效商品同步任务异常", e);
        }
    }

    /**
     * 每2分钟同步一次更新商品
     */
    @Scheduled(cron = "0 */2 * * * ?")
    public void syncUpdatedGoods() {
        // 记录任务开始日志
        log.info("开始执行商品更新任务...");
        try {
            // 调用更新商品同步服务
            goodsService.syncUpdatedGoods();
            // 记录任务完成日志
            log.info("商品更新任务完成");
        } catch (Exception e) {
            // 记录任务异常日志
            log.error("商品更新任务异常", e);
        }
    }
}
