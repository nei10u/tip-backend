package com.nei10u.tip.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

/**
 * 提现记录实体类
 */
@Data
@TableName("withdraw_records")
public class WithdrawRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId; // 用户ID
    private Double amount; // 提现金额
    private String realName; // 真实姓名
    private String account; // 提现账号(支付宝/微信)
    private String type; // 账号类型 (ALIPAY/WECHAT)

    private Integer status; // 状态: 0-审核中, 1-已打款, 2-驳回
    private String remark; // 备注(驳回原因等)

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
