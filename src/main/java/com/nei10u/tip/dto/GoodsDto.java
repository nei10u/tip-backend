package com.nei10u.tip.dto;

import lombok.Data;

/**
 * 商品DTO
 */
@Data
public class GoodsDto {

    private String goodsId;
    private String title;
    private String description;
    private String mainPic;

    private Double price;
    private Double originalPrice;
    private Double couponAmount;

    private Double commissionRate;
    private Double estimatedCommission;

    private String platform;
    private String goodsUrl;
    private String promotionUrl;

    private Integer salesVolume;
    private String shopName;
}
