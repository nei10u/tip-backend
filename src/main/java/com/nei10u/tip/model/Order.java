package com.nei10u.tip.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

/**
 * 订单实体类
 * <p>
 * 存储所有 CPS 平台 (淘宝/京东/拼多多/唯品会) 的推广订单数据。
 * 数据通常通过定时任务或回调从第三方 API 同步而来。
 * 
 * 对应数据库表: orders
 */
@Data
@TableName("orders")
public class Order {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // ==========================================
    // 订单核心信息
    // ==========================================

    /**
     * 本地系统订单号
     * 全局唯一，用于内部流转。
     */
    private String orderSn;

    /**
     * 第三方平台订单号
     * 对应淘宝/京东侧的订单 ID，用于对账和更新状态。
     */
    private String dsOrderSn;

    /**
     * 订单标题 / 商品标题
     */
    private String orderTitle;

    /**
     * 商品主图 URL
     */
    private String img;

    // ==========================================
    // 归属信息
    // ==========================================

    /**
     * 推广位 ID / 用户标识 (SID)
     * 用于关联订单到具体的用户 (User.relationId 或 User.specialId)。
     * 通过此字段判断佣金归属于谁。
     */
    private String sid;

    /**
     * 平台类型编号
     * 1: 淘宝
     * 2: 京东
     * 3: 拼多多
     * 4: 唯品会
     * 5: 抖音
     */
    private Integer typeNo;

    /**
     * 联盟平台标识（DTK/ZTK/...）
     */
    private String unionPlatform;

    /**
     * 平台名称
     * (冗余字段) 如 "淘宝", "京东"。
     */
    private String typeName;

    // ==========================================
    // 金额与佣金
    // ==========================================

    /**
     * 订单成交金额 (用户支付金额)
     */
    private Double orderPrice;

    /**
     * 实际支付金额 (扣除红包等优惠后的金额)
     */
    private Double payPrice;

    /**
     * 佣金比率 (%)
     * 平台给出的佣金比例，如 10.5 代表 10.5%。
     */
    private Double shareRate;

    /**
     * 预估佣金 (Share Fee)
     * 平台预估会结算给推广者的佣金金额。
     * 计算公式通常为: payPrice * shareRate
     */
    private Double shareFee;

    /**
     * 佣金基数（gross）
     *
     * 说明：联盟平台返回的原始“可分配金额口径”（可能是佣金/推广金额/利润），
     * 用于拆分固定扣点/平台盈利/用户可得。
     */
    private Double grossShareFee;

    /** 固定扣点比例（例如 0.1） */
    private Double baseDeductionRate;
    /** 固定扣点金额 */
    private Double baseDeductionFee;

    /** 本站平台盈利比例（可配置） */
    private Double platformProfitRate;
    /** 本站平台盈利金额 */
    private Double platformProfitFee;

    /**
     * 用户折扣比例
     * 系统配置的给用户的分佣比例，如 0.6 表示将佣金的 60% 分给用户。
     */
    private Double userDiscount;

    /**
     * 订单折扣力度 (可选)
     */
    private Double orderDiscount;

    // ==========================================
    // 状态管理
    // ==========================================

    /**
     * 订单状态 (标准化)
     * 0: 创建/未支付
     * 1: 已支付
     * 2: 已结算
     * 3: 已失效
     */
    private Byte orderStatus;

    /**
     * 状态描述
     * 第三方原始状态描述，如 "订单结算", "订单失效"。
     */
    private String statusContent;

    /**
     * 真实状态 (平台原始状态码)
     * 例如淘宝的 12, 13, 14 等状态码。
     */
    private Integer orderRealStatus;

    /**
     * 退款状态
     * 0: 无退款
     * 1: 维权中
     * 2: 维权成功
     */
    private Integer refundStatus;

    /**
     * 订单锁定状态
     * 0: 未锁定
     * 1: 已锁定 (不可操作，通常用于结算中或风控拦截)
     */
    private Integer orderLock;

    // ==========================================
    // 关键时间点
    // ==========================================

    /**
     * 创建时间
     * 订单入库时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 支付时间
     * 用户在电商平台完成支付的时间。
     */
    private Date payTime;

    /**
     * 结算时间
     * 联盟平台实际结算佣金的时间（通常是确认收货后次月20日）。
     */
    private Date earnTime;

    /**
     * 修改时间
     * 第三方数据变更的时间。
     */
    private Date modifyTime;

    /**
     * 记录更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 支付月份
     * 格式 yyyyMM，用于按月统计报表。
     */
    private String payMonth;

    /**
     * 预估结算日期
     * 格式 yyyy-MM-dd。
     */
    private String estimateDate;

    // ==========================================
    // 风控与备注
    // ==========================================

    /**
     * 处罚原因 / 违规说明
     * 如果订单被判定为违规推广，此处记录原因。
     */
    private String punishReason;
}
