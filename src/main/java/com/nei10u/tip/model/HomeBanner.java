package com.nei10u.tip.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 首页轮播图实体类
 * <p>
 * 对应数据库表 home_banner，用于存储首页顶部的轮播广告配置。
 * 使用 MyBatis Plus 进行 ORM 映射。
 */
@Data // Lombok 注解：自动生成 Getter, Setter, toString, equals, hashCode 方法，简化代码
@TableName("public.home_banner") // 明确绑定 public schema，避免 search_path 造成读写漂移
public class HomeBanner {

    /**
     * 主键 ID
     * <p>
     * 
     * @TableId 注解指定主键策略。
     *          IdType.AUTO 表示使用数据库自增主键（在 PostgreSQL 中通常对应 SERIAL/BIGSERIAL 类型）。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 轮播图标题
     * 用于后台管理展示或前端辅助显示（如无障碍标签）。
     */
    private String title;

    /**
     * 图片地址 URL
     * 必须是完整的网络路径（如 https://...），前端将直接加载此 URL。
     */
    private String imageUrl;

    /**
     * 跳转链接
     * 点击轮播图后的跳转目标。
     * 可以是 Web URL (https://...) 或 App 内部路由 (app://...)。
     */
    private String linkUrl;

    /**
     * 排序权重
     * 数值越小越靠前显示。用于控制轮播图的播放顺序。
     */
    private Integer sortOrder;

    /**
     * 状态
     * 1: 启用 (Active) - 前端可见
     * 0: 禁用 (Inactive) - 前端不可见
     * 业务逻辑中通常只查询 status = 1 的记录。
     */
    private Integer status;

    /**
     * 创建时间
     * 记录记录首次创建的时间戳。
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     * 记录记录最后一次被修改的时间戳。
     */
    private LocalDateTime updatedTime;
}
