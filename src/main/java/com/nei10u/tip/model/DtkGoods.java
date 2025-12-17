package com.nei10u.tip.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 淘宝商品实体类
 */
@Data
@TableName("dtk_goods")
public class DtkGoods implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 商品ID
     */
    private String goodsId;

    /**
     * 商品标题
     */
    private String title;

    /**
     * 短标题
     */
    private String dtitle;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 主图
     */
    private String mainPic;

    /**
     * 营销主图
     */
    private String marketingMainPic;

    /**
     * 券后价
     */
    private BigDecimal price;

    /**
     * 原价
     */
    private BigDecimal originalPrice;

    /**
     * 优惠券金额
     */
    private BigDecimal couponPrice;

    /**
     * 优惠券链接
     */
    private String couponLink;

    /**
     * 优惠券开始时间
     */
    private LocalDateTime couponStartTime;

    /**
     * 优惠券结束时间
     */
    private LocalDateTime couponEndTime;

    /**
     * 佣金比例
     */
    private BigDecimal commissionRate;

    /**
     * 月销量
     */
    private Integer salesVolume;

    /**
     * 店铺类型 (1:天猫, 0:淘宝)
     */
    private Integer shopType;

    /**
     * 店铺名称
     */
    private String shopName;

    /**
     * 店铺等级
     */
    private Integer shopLevel;

    /**
     * 品牌名称
     */
    private String brandName;

    /**
     * 平台标识
     */
    private String platform;

    /**
     * 活动类型
     */
    private Integer activityType;

    /**
     * 活动开始时间
     */
    private LocalDateTime activityStartTime;

    /**
     * 活动结束时间
     */
    private LocalDateTime activityEndTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 状态 (1:正常, 0:失效)
     */
    private Integer status;
}

