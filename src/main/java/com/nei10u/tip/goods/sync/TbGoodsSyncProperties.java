package com.nei10u.tip.goods.sync;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * TB 本地商品库同步参数（application.yml: app.goods.sync.tb.*）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.goods.sync.tb")
public class TbGoodsSyncProperties {

    /**
     * MyBatis batch 提交大小（过大可能占用更多内存；过小则批处理收益下降）
     */
    private int batchSize = 300;

    /**
     * 是否启用“单实例互斥锁”，避免同一 JVM 内多个调度/触发并发执行同步逻辑
     *（多实例部署若需要全局互斥，需要引入分布式锁）
     */
    private boolean lockEnabled = true;

    /**
     * tryLock 超时时间（毫秒）；0 表示立即尝试，失败则跳过本次执行
     */
    private long lockTimeoutMs = 0;
}

