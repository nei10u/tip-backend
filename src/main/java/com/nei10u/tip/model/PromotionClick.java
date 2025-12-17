package com.nei10u.tip.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("promotion_click")
public class PromotionClick {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long infoId; // 关联 PromotionInfo id
    private Long userId; // 点击者ID，可能为空

    @TableField(fill = FieldFill.INSERT)
    private Date clickTime;

    private String ip;
}
