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
}
