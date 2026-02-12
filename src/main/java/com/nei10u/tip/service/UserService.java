package com.nei10u.tip.service;

import com.nei10u.tip.dto.UserDto;
import com.nei10u.tip.model.User;

/**
 * 用户服务接口 (基于参考实现)
 */
public interface UserService {

    /**
     * 用户注册
     */
    UserDto register(User user);

    /**
     * 根据ID获取用户
     */
    UserDto getUserById(Long id);

    /**
     * 根据渠道关系ID获取用户
     */
    UserDto getUserByRelationId(Long relationId);

    /**
     * 根据专用ID获取用户
     */
    UserDto getUserBySpecialId(Long specialId);

    /**
     * 根据淘宝用户ID获取用户
     */
    UserDto getUserByTbUserId(String tbUserId);

    /**
     * 根据UnionID获取用户
     */
    UserDto getUserByUnionId(String unionId);

    /**
     * 根据拼多多PID获取用户
     */
    UserDto getUserByPddPid(String pddPid);

    /**
     * 根据京东授权/绑定ID获取用户
     */
    UserDto getUserByJdAuthId(String jdAuthId);

    /**
     * 根据手机号获取用户
     */
    UserDto getUserByPhone(String phone);

    /**
     * 更新用户信息
     */
    int updateUser(User user);

    /**
     * 更新拼多多PID
     */
    int updatePddPid(Long userId, String pddPid);

    /**
     * 更新拼多多状态
     */
    int updatePddStatus(Long userId, Boolean status);

    /**
     * 绑定CPS信息
     * 
     * @param userId   用户ID
     * @param platform 平台 (tb/jd/pdd)
     * @param authId   授权ID (relatonId/specialId/pid)
     */
    boolean bindCps(Long userId, String platform, String authId);

    /**
     * 微信登录
     * 
     * @param openId     小程序OpenID
     * @param unionId    UnionID (可选)
     * @param sessionKey 会话密钥
     */
    UserDto loginByWechat(String openId, String unionId, String sessionKey);

    /**
     * 账号密码登录（手机号 + 密码）
     */
    UserDto loginByPassword(String phone, String password);

    /**
     * 短信验证码登录（手机号 + 验证码）
     */
    UserDto loginBySms(String phone, String code);

    /**
     * 短信验证码注册（手机号 + 验证码 + 密码）
     */
    UserDto registerBySms(String phone, String code, String password);

    /**
     * 绑定手机号（微信登录后补齐手机号）
     *
     * @param userId 当前登录用户 ID
     * @param phone  手机号
     * @param code   短信验证码
     */
    UserDto bindPhone(Long userId, String phone, String code);

    /**
     * 通过 token 获取当前用户（用于 Bearer token 鉴权场景）。
     *
     * 说明：当前项目将 token 作为“服务端可控会话串”回写到 users.token 中；
     * 因此这里采用 DB 查找，而不是纯 JWT 本地验签。
     */
    UserDto getUserByToken(String token);
}
