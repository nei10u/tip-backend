package com.nei10u.tip.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("promotion_info")
public class PromotionInfo {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String goodsId;
    private String platform; // tb, jd, pdd, vip
    private String promotionUrl;
    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    private Date expireTime;
}
