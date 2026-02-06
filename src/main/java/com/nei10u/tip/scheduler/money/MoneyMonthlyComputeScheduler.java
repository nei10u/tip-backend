package com.nei10u.tip.scheduler.money;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nei10u.tip.mapper.MoneyChangeMapper;
import com.nei10u.tip.mapper.OrderMapper;
import com.nei10u.tip.model.MoneyChange;
import com.nei10u.tip.model.Order;
import com.nei10u.tip.service.MoneyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 月度结算任务（最终结算/入账对账）：
 * <p>
 * 目标：
 * - 订单同步（insertOrUpdateOrder）只负责落库，不触发入账
 * - 结算任务周期性扫描订单，将 orders.credited_fee 收敛到“应入账金额 desiredCredit”
 * - delta = desiredCredit - credited_fee 通过 money_change 幂等入账/冲账，并更新 money.balance
 *
 * <p>
 * 幂等策略：
 * - uuid = "10:{orderSn}:{oldCredited}->{desired}"，保证相同状态下重复跑不会重复入账
 * - 即使某次崩溃导致 credited_fee 未回写，uuid 也能阻止重复更新余额；下次会补齐 credited_fee 回写
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MoneyMonthlyComputeScheduler {

    private final OrderMapper orderMapper;
    private final MoneyService moneyService;
    private final MoneyChangeMapper moneyChangeMapper;

    @Value("${app.settlement.monthly.page-size:500}")
    private int pageSize;

    /**
     * 默认：每月 24 日 01:00 进行一次“最终结算/对账入账”。
     * 可通过 app.settlement.monthly.cron 覆盖。
     */
    @Scheduled(cron = "${app.settlement.monthly.cron:0 0 1 24 * ?}")
    public void scheduled() {
        long start = System.currentTimeMillis();
        int changed = runReconcileLoop();
        log.info("MoneyMonthlyComputeScheduler done: changedOrders={}, costMs={}", changed, System.currentTimeMillis() - start);
    }

    /**
     * 单独抽出以便测试/复用。
     * <p>
     * 注意：这里用“按 id 递增”做 keySet 分页，避免 offset 在大表下性能退化。
     */
    @Transactional
    protected int runReconcileLoop() {
        long lastId = 0L;
        int changedOrders = 0;

        while (true) {
            List<Order> batch = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                    .isNotNull(Order::getUserId)
                    .gt(Order::getId, lastId)
                    .orderByAsc(Order::getId)
                    .last("limit " + Math.max(1, pageSize)));

            if (batch == null || batch.isEmpty()) break;

            for (Order order : batch) {
                lastId = Math.max(lastId, order.getId() == null ? lastId : order.getId());
                if (order.getUserId() == null) continue;

                double oldCredited = order.getCreditedFee() == null ? 0.0d : order.getCreditedFee();
                double desired = computeDesiredCredit(order);
                double delta = desired - oldCredited;

                boolean needWriteCredited = Math.abs(desired - oldCredited) >= 0.01d || (order.getCreditedFee() == null && desired != 0.0d);
                if (!needWriteCredited) {
                    continue;
                }

                // 先尝试幂等插入流水（如果 delta==0 只回写 credited_fee，不动余额）
                if (Math.abs(delta) >= 0.01d) {
                    String uuid = "10:" + order.getOrderSn() + ":" + format2(oldCredited) + "->" + format2(desired);
                    if (tryInsertMoneyChange(order.getUserId(), order.getOrderSn(), delta, uuid)) {
                        moneyService.updateBalance(String.valueOf(order.getUserId()), delta);
                        log.info("Monthly reconcile: sn={}, userId={}, delta={}, credited(old)->desired({}->{})",
                                order.getOrderSn(), order.getUserId(), delta, format2(oldCredited), format2(desired));
                    } else {
                        log.info("Monthly reconcile skipped (idempotent): sn={}, userId={}, delta={}, credited(old)->desired({}->{})",
                                order.getOrderSn(), order.getUserId(), delta, format2(oldCredited), format2(desired));
                    }
                }

                // 回写 orders.credited_fee 作为锚点（无论是否插入流水成功，都应收敛到 desired）
                orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                        .eq(Order::getId, order.getId())
                        .set(Order::getCreditedFee, desired)
                        .set(Order::getUpdateTime, new Date()));
                changedOrders++;
            }
        }

        return changedOrders;
    }

    /**
     * 最终结算口径（与历史实现保持一致）：
     * - 锁单：0
     * - 失效：0（若历史已入账，则 delta 为负自动冲账）
     * - 已结算：shareFee
     * - 其他：0
     */
    private double computeDesiredCredit(Order order) {
        if (order == null) return 0.0d;
        if (order.getOrderLock() != null && order.getOrderLock() == 1) return 0.0d;
        if (order.getOrderStatus() != null && order.getOrderStatus() == 3) return 0.0d;
        if (order.getOrderStatus() != null && order.getOrderStatus() == 2) {
            return order.getShareFee() == null ? 0.0d : order.getShareFee();
        }
        return 0.0d;
    }

    private static String format2(Double v) {
        if (v == null) return "0.00";
        return String.format(Locale.ROOT, "%.2f", v);
    }

    private boolean tryInsertMoneyChange(Long userId, String orderSn, Double amount, String uuid) {
        if (userId == null || orderSn == null || orderSn.isBlank() || amount == null) return false;

        MoneyChange mc = new MoneyChange();
        mc.setUserId(userId);
        mc.setOrderSn(orderSn);
        mc.setChangeType((short) 10);
        mc.setAmount(amount);
        mc.setUuid(uuid);
        mc.setCreateTime(new Date());

        return moneyChangeMapper.insertIgnore(mc) > 0;
    }
}


