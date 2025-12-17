package com.nei10u.tip.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

/**
 * 资金账户实体类
 * <p>
 * 用户的核心资产账户，记录余额、收入和提现统计。
 * 资金安全至关重要，涉及金额的修改通常需要数据库锁或乐观锁支持。
 * 
 * 对应数据库表: money
 */
@Data
@TableName("money")
public class Money {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户 ID
     * 关联 User.id，建立一对一关系。
     */
    private String userId;

    /**
     * 可用余额
     * 用户可立即提现的金额。
     */
    private Double balance;

    /**
     * 冻结金额
     * 提现申请中或风控冻结的金额，不可用。
     */
    private Double frozen;

    /**
     * 总收入 (累计收益)
     * 历史所有确认收货且已结算的佣金总和。
     * 只增不减，用于统计展示。
     */
    private Double totalIncome;

    /**
     * 总提现金额
     * 历史已打款成功的总金额。
     */
    private Double totalWithdraw;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
