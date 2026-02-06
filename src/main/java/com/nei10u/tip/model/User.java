package com.nei10u.tip.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

/**
 * 用户实体类
 * <p>
 * 核心用户模型，存储用户的基本信息、绑定关系和资金概况字段。
 * 采用了"胖模型"设计，包含了 CPS (Cost Per Sale) 业务所需的各种平台关联 ID。
 * 
 * 对应数据库表: users
 */
@Data // Lombok: 自动生成 Getter/Setter/ToString/Equals/HashCode
@TableName("users") // MyBatis Plus: 映射到 users 表
public class User {

    /**
     * 用户主键 ID
     * 数据库自增 ID，全局唯一标识。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // ==========================================
    // CPS (淘宝客/京东联盟) 核心关联字段
    // ==========================================

    /**
     * 渠道关系 ID (Relation ID)
     * <p>
     * 淘宝联盟的高级权限 ID，用于追踪用户的推广行为。
     * 当用户通过渠道授权后获取，是实现分佣的关键字段。
     */
    private Long relationId;

    /**
     * 专用 ID (Special ID)
     * <p>
     * 类似于 Relation ID，通常用于私域流量管理或特定权限分组。
     */
    private Long specialId;

    /**
     * 淘宝用户 ID (TB User ID)
     * <p>
     * 用户的淘宝账号唯一标识（通常是加密后的字符串）。
     * 用于绑定淘宝账号，查询订单归属。
     */
    private String tbUserId;

    /**
     * 拼多多 PID (Pinduoduo Promotion ID)
     * <p>
     * 拼多多推广位 ID，用于标识该用户推广的订单。
     */
    private String pddPid;

    // ==========================================
    // 京东联盟字段（独立于 TB）
    // ==========================================

    /**
     * 京东 CPS 授权/绑定 ID（项目侧自定义存储）
     * <p>
     * 说明：不同接入方可能使用 unionId / 授权key / positionId 等作为“归因/绑定标识”。
     * 这里统一存为字符串，供转链/校验授权使用。
     */
    @TableField("jd_auth_id")
    private String jdAuthId;

    /**
     * 京东绑定状态
     */
    @TableField("jd_status")
    private Boolean jdStatus;

    // ==========================================
    // 微信生态字段
    // ==========================================

    /**
     * 微信公众号 OpenID
     * <p>
     * 用户在微信公众号下的唯一标识。用于发送模板消息、公众号登录。
     */
    private String mpOpenId;

    /**
     * 微信小程序 OpenID
     * <p>
     * 用户在微信小程序下的唯一标识。用于小程序登录、支付。
     */
    private String mnOpenId;

    /**
     * 微信 UnionID
     * <p>
     * 微信开放平台下的跨应用唯一标识。
     * 只有拥有 UnionID，才能打通公众号和小程序的用户账号。
     */
    private String unionId;

    /**
     * 用户昵称
     * 通常从微信获取，或用户自行设置。
     */
    private String nickname;

    /**
     * 用户头像 URL
     * 通常从微信获取，或用户自行上传。
     */
    private String avatarUrl;

    // ==========================================
    // 支付与认证字段
    // ==========================================

    /**
     * 支付宝账号
     * 用于提现打款。
     */
    @TableField("alipay_uid")
    private String aliPayAccount;

    /**
     * 支付宝真实姓名
     * 提现时校验用，必须与支付宝实名信息一致。
     */
    private String aliPayName;

    /**
     * 身份证姓名
     * 实名认证字段。
     */
    @TableField("real_name")
    private String idCardName;

    /**
     * 身份证号码
     * 实名认证字段，需加密存储或脱敏展示。
     */
    private String idCardNum;

    // ==========================================
    // 基础信息与状态
    // ==========================================

    /**
     * 手机号码
     * 用户唯一标识之一，用于短信验证码登录/通知。
     */
    private String phone;

    /**
     * 邮箱（可选）
     */
    private String email;

    /**
     * 登录 Token
     * 用于 API 鉴权。通常存储 JWT 字符串或 Session ID。
     */
    private String token;

    /**
     * 用户状态
     * 1: 正常
     * 0: 禁用/黑名单
     */
    private Integer status;

    /**
     * 公众号关注状态
     * true: 已关注
     * false: 未关注
     */
    private Boolean mpStatus;

    /**
     * 拼多多绑定状态
     * true: 已绑定
     * false: 未绑定
     */
    private Boolean pddStatus;

    // ==========================================
    // 冗余统计字段 (为了性能优化，有时会直接存在用户表)
    // ==========================================

    /**
     * 用户折扣/返佣比例
     * 例如 0.8 表示给用户返佣 80%。
     */
    private Double userDiscount;

    /**
     * 总实际费用 (累计消费)
     */
    private Double totalActualFee;

    /**
     * 冻结费用 (待结算佣金)
     */
    private Double frozenFee;

    // ==========================================
    // 系统字段
    // ==========================================

    /**
     * 创建时间
     * 
     * @TableField(fill = FieldFill.INSERT) - MyBatis Plus 自动填充
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     * 
     * @TableField(fill = FieldFill.INSERT_UPDATE) - MyBatis Plus 自动填充
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
