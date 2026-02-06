package com.nei10u.tip.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 资金流水（用于审计与幂等）。
 * 对应表：money_change
 */
@Data
@TableName("money_change")
public class MoneyChange {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String orderSn;

    /**
     * 变更类型：
     * 1-订单结算入账
     * 2-订单冲账（已结算->失效/退款导致扣减）
     * 10-订单入账对账调整（统一覆盖：结算/失效/锁单/差额）
     */
    private Short changeType;

    /** 变更金额（入账为正，冲账为负） */
    private Double amount;

    /** 幂等键（建议：type:orderSn） */
    private String uuid;

    private Date createTime;
}


