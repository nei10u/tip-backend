package com.nei10u.tip.dto;

import lombok.Data;
import java.util.Date;

/**
 * 订单DTO
 */
@Data
public class OrderDto {

    private Long id;
    private Long userId;
    private String orderSn;
    private String dsOrderSn;
    private String orderTitle;
    private String img;

    private String sid;

    private Integer typeNo;
    private String typeName;

    private Double orderPrice;
    private Double payPrice;
    private Double shareRate;
    private Double shareFee;

    // 盈利展示（可选）
    private String unionPlatform;
    private Double grossShareFee;
    private Double baseDeductionRate;
    private Double baseDeductionFee;
    private Double platformProfitRate;
    private Double platformProfitFee;
    private Double userDiscount;
    private Double orderDiscount;

    private Byte orderStatus;
    private String statusContent;

    private Date createTime;
    private Date payTime;
    private Date earnTime;

    private String payMonth;
    private String estimateDate;
}
