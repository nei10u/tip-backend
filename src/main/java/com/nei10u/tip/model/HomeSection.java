package com.nei10u.tip.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 首页活动专区实体类
 * <p>
 * 对应数据库表 home_section，用于配置首页中部的活动板块（如"聚划算"、"百亿补贴"等）。
 * 通常以大图卡片的形式展示。
 */
@Data // Lombok 注解：通过字节码增强技术自动生成 Java Bean 方法
@TableName("home_section") // MyBatis Plus 注解：映射数据库表 home_section
public class HomeSection {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 专区标题
     * 可用于埋点统计或前端展示标题栏。
     */
    private String title;

    /**
     * 专区入口图 URL
     * 这里的图片通常是长方形的 Banner 或卡片图。
     */
    private String imageUrl;

    /**
     * 点击跳转链接
     * 指向具体的活动页面。
     */
    private String linkUrl;

    /**
     * 活动提供方 / 来源
     * 标识该活动属于哪个平台，用于前端做特殊的样式区分或统计。
     * 枚举值示例：
     * - taobao: 淘宝/天猫
     * - jd: 京东
     * - vip: 唯品会
     * - general: 通用/自营
     */
    private String provider;

    /**
     * 排序值
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
