package com.nei10u.tip.controller;

import com.nei10u.tip.dto.UserDto;
import com.nei10u.tip.model.User;
import com.nei10u.tip.service.RealNameService;
import com.nei10u.tip.service.UserService;
import com.nei10u.tip.service.WechatService;
import com.nei10u.tip.vo.ResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 * <p>
 * 处理用户相关的 HTTP 请求，包括注册、登录（微信一键登录）、实名认证、信息查询与更新等。
 * 作为系统核心模块，负责用户身份的建立与维护。
 */
@Tag(name = "用户接口") // Swagger 文档分组标签
@RestController // 声明为 REST 控制器，返回值默认为 JSON
@RequestMapping("/api/user") // 基础路由路径
@RequiredArgsConstructor // Lombok: 自动生成包含 final 字段的构造函数，实现依赖注入
public class UserController {

    // 依赖注入：用户业务服务
    private final UserService userService;
    // 依赖注入：微信服务（处理与微信服务器的交互）
    private final WechatService wechatService;
    // 依赖注入：实名认证服务
    private final RealNameService realNameService;

    /**
     * 用户注册接口
     * <p>
     * 通常用于手机号注册或后台创建用户。
     *
     * @param user 用户实体对象，包含注册信息（如手机号、密码等），由 RequestBody 接收 JSON 数据
     * @return ResponseVO<UserDto> 包含注册后的用户信息（脱敏）
     */
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public ResponseVO<UserDto> register(@RequestBody User user) {
        return ResponseVO.success(userService.register(user));
    }

    /**
     * 微信登录/注册接口
     * <p>
     * 利用微信小程序的 code 换取 openid/unionid，并自动完成登录或注册流程。
     * 这是小程序端最常用的登录方式。
     *
     * @param code 微信小程序端通过 wx.login 获取的临时登录凭证
     * @return ResponseVO<UserDto> 包含用户 Token 和基本信息
     */
    @Operation(summary = "微信登录")
    @PostMapping("/login/wechat")
    public ResponseVO<UserDto> loginByWechat(@RequestParam String code) {
        // 1. 调用微信接口获取 Session (OpenID, UnionID, SessionKey)
        // 这一步会向微信服务器发起请求
        cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult session = wechatService.login(code);

        // 2. 调用业务服务进行登录或注册
        // 如果数据库中不存在该 OpenID，则自动创建新用户
        UserDto userDto = userService.loginByWechat(session.getOpenid(), session.getUnionid(), session.getSessionKey());

        return ResponseVO.success(userDto);
    }

    /**
     * 实名认证接口
     * <p>
     * 验证用户的真实姓名和身份证号是否匹配。
     * 在提现或高风险操作前通常需要此步骤。
     *
     * @param userId   用户 ID
     * @param realName 真实姓名
     * @param idCard   身份证号
     * @return ResponseVO<Boolean> 认证是否成功
     */
    @Operation(summary = "实名认证")
    @PostMapping("/realname")
    public ResponseVO<Boolean> verifyRealName(
            @RequestParam Long userId,
            @RequestParam String realName,
            @RequestParam String idCard) {
        // 1. 调用第三方实名认证服务进行校验
        boolean verified = realNameService.verify(realName, idCard);

        if (verified) {
            // 2. 认证通过后，更新本地数据库中的用户信息
            // 仅更新必要字段，保证数据一致性
            User user = new User();
            user.setId(userId);
            user.setIdCardName(realName);
            user.setIdCardNum(idCard);
            userService.updateUser(user);
        }

        return ResponseVO.success(verified);
    }

    /**
     * 根据 ID 获取用户信息
     *
     * @param id 用户主键 ID
     * @return UserDto 用户详情
     */
    @Operation(summary = "获取用户信息")
    @GetMapping("/{id}")
    public ResponseVO<UserDto> getUserById(@PathVariable Long id) {
        UserDto userDto = userService.getUserById(id);
        return ResponseVO.success(userDto);
    }

    /**
     * 根据 UnionID 获取用户
     * <p>
     * UnionID 是微信生态下跨应用的唯一标识。
     *
     * @param unionId 微信 UnionID
     * @return UserDto 用户详情
     */
    @Operation(summary = "根据UnionID获取用户")
    @GetMapping("/unionId/{unionId}")
    public ResponseVO<UserDto> getUserByUnionId(@PathVariable String unionId) {
        UserDto userDto = userService.getUserByUnionId(unionId);
        return ResponseVO.success(userDto);
    }

    /**
     * 更新用户信息
     * <p>
     * 支持更新昵称、头像等非敏感信息。
     *
     * @param user 包含待更新字段的用户对象
     * @return Integer 更新影响的行数
     */
    @Operation(summary = "更新用户信息")
    @PutMapping
    public ResponseVO<Integer> updateUser(@RequestBody User user) {
        int count = userService.updateUser(user);
        return ResponseVO.success(count);
    }

    /**
     * 绑定 CPS 平台账号
     * <p>
     * 将用户与淘宝/京东/拼多多的联盟账号关联，以便追踪分佣。
     *
     * @param userId   用户 ID
     * @param platform 平台标识 (tb, jd, pdd, vip)
     * @param authId   平台侧的授权 ID (如淘宝的 relation_id)
     * @return Boolean 绑定是否成功
     */
    @Operation(summary = "绑定CPS信息")
    @PostMapping("/bind/cps")
    public ResponseVO<Boolean> bindCps(
            @RequestParam Long userId,
            @RequestParam String platform,
            @RequestParam String authId) {
        boolean success = userService.bindCps(userId, platform, authId);
        return ResponseVO.success(success);
    }
}
