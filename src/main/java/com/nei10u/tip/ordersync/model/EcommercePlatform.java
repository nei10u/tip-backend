package com.nei10u.tip.ordersync.model;

/**
 * 具体电商平台（订单真实发生的平台）。
 * <p>
 * 注意：这里的 platform 与“联盟平台”不同：
 * - 联盟平台：你调用谁的 API（DTK/ZTK/…）
 * - 电商平台：订单来自哪里（TB/JD/PDD/VIP/…）
 */
public enum EcommercePlatform {
    UNKNOWN(-1, "未知"),
    TB(1, "淘宝"),
    JD(2, "京东"),
    PDD(3, "拼多多"),
    VIP(4, "唯品会"),
    DY(5, "抖音"),
    MT(6, "美团");

    private final int typeNo;
    private final String displayName;

    EcommercePlatform(int typeNo, String displayName) {
        this.typeNo = typeNo;
        this.displayName = displayName;
    }

    public int getTypeNo() {
        return typeNo;
    }

    public String getDisplayName() {
        return displayName;
    }
}

