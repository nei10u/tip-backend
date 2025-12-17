package com.nei10u.tip.ordersync.model;

/**
 * 联盟平台（聚合平台）标识。
 *
 * 联盟平台负责提供订单查询 API，但订单可能来自不同电商平台（淘宝/京东/拼多多/唯品会等）。
 */
public enum UnionPlatform {
    /** 大淘客 */
    DTK,
    /** 折淘客 */
    ZTK
}

