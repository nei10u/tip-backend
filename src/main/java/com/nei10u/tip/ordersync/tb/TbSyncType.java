package com.nei10u.tip.ordersync.tb;

import lombok.Getter;

/**
 * TB 同步类型（对齐 legacy OrderSyncTbUtil.SyncType -> queryType 映射）。
 * <p>
 * queryType:
 * 1：按照订单淘客创建时间查询，2:按照订单淘客付款时间查询，3:按照订单淘客结算时间查询，4:按照订单更新时间；
 */
@Getter
public enum TbSyncType {
    MINUTE(4),
    DAY(4),
    MONTH_UPDATE(4),
    MONTH_CREATE(1),
    MONTH_COMPLETE(3),
    PAY(2);

    private final int queryType;

    TbSyncType(int queryType) {
        this.queryType = queryType;
    }

}


