package com.nei10u.tip.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 首页功能菜单实体类 (金刚区)
 * <p>
 * 对应数据库表 home_menu，用于存储首页中部的图标网格导航（通常是2行5列或1行滑动）。
 * 每个菜单项包含图标、标签和点击动作。
 */
@Data // Lombok 注解：自动生成 Getter, Setter, toString, equals, hashCode 等样板代码
@TableName("home_menu") // MyBatis Plus 注解：建立实体类与数据库表 home_menu 的映射关系
public class HomeMenu {

    /**
     * 主键 ID
     * 数据库自增唯一标识。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 菜单显示的文字标签
     * 例如："淘宝热销"、"京东精选"、"限时秒杀"。
     * 建议不超过 5 个汉字，以免前端换行影响布局。
     */
    private String label;

    /**
     * 菜单图标的 URL 地址
     * 建议使用正方形图片（如 PNG/WebP），支持透明背景。
     */
    private String iconUrl;

    /**
     * 点击跳转的目标链接
     * 具体的路由地址，由前端路由解析器处理。
     */
    private String linkUrl;

    /**
     * 链接类型
     * 用于区分不同的跳转逻辑：
     * - route: App 内部原生页面跳转
     * - webview: 打开内置 H5 浏览器
     * - deep_link: 唤起第三方 App (如直接打开淘宝 App)
     */
    private String type;

    /**
     * 排序顺序
     * 决定图标在网格中的排列先后。数值越小排在越前面。
     */
    private Integer sortOrder;

    /**
     * 启用状态
     * 1 表示启用，0 表示禁用。
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
}
