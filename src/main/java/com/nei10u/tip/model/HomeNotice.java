package com.nei10u.tip.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 首页公告实体类 (跑马灯)
 * <p>
 * 对应数据库表 home_notice，用于存储首页滚动的文本公告。
 */
@Data
@TableName("public.home_notice") // 明确绑定 public schema，避免 search_path 造成读写漂移
public class HomeNotice {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 公告内容
     * 显示在跑马灯中的文本。
     */
    private String content;

    /**
     * 跳转链接
     * 点击公告后的跳转目标（可选）。
     */
    private String linkUrl;

    /**
     * 排序权重
     */
    private Integer sortOrder;

    /**
     * 状态 (1: 启用, 0: 禁用)
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
}

