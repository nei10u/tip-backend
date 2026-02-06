package com.nei10u.tip.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 淘宝退款证据链（最小实现：保存原始 JSON）。
 *
 * 对应表：tb_order_refund
 */
@Data
@TableName("tb_order_refund")
public class TbOrderRefund {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 淘宝/联盟侧 tradeId（对应 orders.ds_order_sn） */
    private String tradeId;

    /** 本站 orderSn（可选，便于反查） */
    private String orderSn;

    /** 原始 JSON（字符串） */
    private String rawJson;

    private Date createTime;
    private Date updateTime;
}


