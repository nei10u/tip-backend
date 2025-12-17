package com.nei10u.tip.ordersync.model;

import com.alibaba.fastjson2.JSONObject;
import lombok.Builder;
import lombok.Value;

/**
 * 联盟平台返回的“原始订单”抽象。
 *
 * - unionPlatform: 你调用的联盟平台
 * - ecommercePlatform: 订单真实发生的平台（联盟平台可能混合返回）
 * - raw: 原始订单 JSON（保留用于审计/字段兜底）
 */
@Value
@Builder
public class RawUnionOrder {
    UnionPlatform unionPlatform;
    EcommercePlatform ecommercePlatform;
    JSONObject raw;
}

