package com.nei10u.tip.ordersync.impl.dtk;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.nei10u.tip.ordersync.model.EcommercePlatform;
import com.nei10u.tip.ordersync.model.RawUnionOrder;
import com.nei10u.tip.ordersync.model.UnionPlatform;
import com.nei10u.tip.ordersync.spi.UnionOrderSyncer;
import com.nei10u.tip.service.DtkApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 大淘客订单同步器实现（联盟平台维度）。
 * <p>
 * 对接大淘客（DaTaoKe）的订单查询接口。
 * 说明：虽然目前 DTK 的订单接口主要返回淘宝订单（tb-service/get-order-details），
 * 但本实现按照“联盟平台可能聚合多种电商平台订单”的通用设计进行处理，根据接口返回的数据动态识别电商平台类型，
 * 便于未来 DTK 聚合更多平台数据时无缝支持。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DtkUnionOrderSyncer implements UnionOrderSyncer {

    private final DtkApiService dtkApiService;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public UnionPlatform unionPlatform() {
        return UnionPlatform.DTK;
    }

    /**
     * 从大淘客接口拉取订单，并标准化为 {@link RawUnionOrder} 列表。
     * 支持自动分页拉取。
     * <p>
     * 推荐使用
     * {@link #processOrders(LocalDateTime, LocalDateTime, java.util.function.Consumer)}
     * 进行流式处理。
     * </p>
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 原始订单列表
     */

    /**
     * 流式拉取订单。
     * <p>
     * 通过分页循环，每获取一页数据就立即回调 consumer 进行处理，避免内存积压。
     * </p>
     */
    @Override
    public void processOrders(LocalDateTime startTime, LocalDateTime endTime,
                              Consumer<List<RawUnionOrder>> batchConsumer) {
        String start = startTime.format(FORMATTER);
        String end = endTime.format(FORMATTER);

        String positionIndex = null;
        int pageSize = 100; // 尽量大，减少请求次数
        int pageNo = 1;
        int maxPages = 500; // 安全熔断防死循环

        while (pageNo <= maxPages) {
            Map<String, Object> params = new HashMap<>();
            // queryType: 1-创建时间, 2-付款时间, 3-结算时间, 4-更新时间
            params.put("queryType", 4);
            params.put("pageSize", pageSize);
            params.put("pageNo", pageNo);
            if (StringUtils.hasText(positionIndex)) {
                params.put("positionIndex", positionIndex);
            }

            String response = dtkApiService.getOrderDetails(start, end, params);
            if (!StringUtils.hasText(response))
                break;

            JSONObject json = null;
            try {
                json = JSON.parseObject(response);
            } catch (Exception e) {
                log.error("Parse DTK response failed: {}", response, e);
                break;
            }

            if (json == null)
                break;

            if (json.containsKey("code") && json.getIntValue("code") != 0) {
                log.warn("DTK order api error: code={}, msg={}", json.getIntValue("code"), json.getString("msg"));
                break;
            }

            // 解析 data 部分
            JSONObject data = json.getJSONObject("data");
            if (data == null) {
                break;
            }

            // 获取订单列表
            JSONArray list = data.getJSONArray("results.publisher_order_dto");
            if (list != null && !list.isEmpty()) {
                List<RawUnionOrder> batch = new ArrayList<>(list.size());
                for (int i = 0; i < list.size(); i++) {
                    JSONObject item = list.getJSONObject(i);
                    log.info("DTK detect item:{}", item);
                    if (item == null)
                        continue;
                    batch.add(RawUnionOrder.builder()
                            .unionPlatform(UnionPlatform.DTK)
                            .ecommercePlatform(detectPlatform(item))
                            .raw(item)
                            .build());
                }
                // 消费当前批次
                if (!batch.isEmpty()) {
                    batchConsumer.accept(batch);
                }
            }

            // 检查分页
            boolean hasNext = data.getBooleanValue("has_next");
            if (!hasNext) {
                break;
            }

            // 更新位点和页码
            positionIndex = data.getString("position_index");
            pageNo++;

            if (!StringUtils.hasText(positionIndex)) {
                // position_index 缺失时的保底逻辑
            }
        }
    }

    /**
     * 根据订单字段特征推断其所属的电商平台。
     * <p>
     * 识别策略：
     * <ol>
     * <li>优先匹配 `typeNo` 或 `type_no` 字段（如果存在且有标准定义）。</li>
     * <li>匹配 `order_type` 字段（DTK 接口标准字段），如 "天猫", "淘宝", "聚划算" 等。</li>
     * <li>次选匹配 `platform`、`type` 或 `source` 字符串字段（兼容其他可能格式）。</li>
     * <li>兜底默认为淘宝（{@link EcommercePlatform#TB}），因为本接口为 `tb-service`。</li>
     * </ol>
     * </p>
     *
     * @param item 单个订单的 JSON 对象
     * @return 识别出的电商平台枚举
     */
    private static EcommercePlatform detectPlatform(JSONObject item) {
        // 1. 尝试 order_type (文档标准字段)
        String platform = item.getString("order_type");
        if (!StringUtils.hasText(platform)) {
            // 2. 次选平台出资方字符串匹配
            platform = item.getString("subsidy_type");
            // 未来若有其他平台关键字可在此扩展
        }
        if (StringUtils.hasText(platform)) {
            if (platform.contains("天猫") || platform.contains("淘宝") || platform.contains("聚划算")
                    || platform.contains("如意淘")) {
                return EcommercePlatform.TB;
            }
        }
        // 3. 默认兜底为未知
        return EcommercePlatform.UNKNOWN;
    }
}
