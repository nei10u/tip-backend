package com.nei10u.tip.service.impl;

import com.nei10u.tip.auth.JwtService;
import com.nei10u.tip.auth.SmsCodeService;
import com.nei10u.tip.dto.UserDto;
import com.nei10u.tip.exception.BusinessException;
import com.nei10u.tip.mapper.UserMapper;
import com.nei10u.tip.model.User;
import com.nei10u.tip.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final SmsCodeService smsCodeService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    @Transactional
    public UserDto register(User user) {
        // 检查用户是否已存在
        if (user.getUnionId() != null) {
            User existing = userMapper.getUserByUnionId(user.getUnionId());
            if (existing != null) {
                throw new BusinessException("USER_EXISTS", "用户已存在");
            }
        }

        applyDefaultsForInsert(user);
        userMapper.insert(user);
        log.info("用户注册成功, id={}", user.getId());

        return convertToDto(user);
    }

    @Override
    public UserDto getUserById(Long id) {
        User user = userMapper.selectById(id);
        return convertToDto(user);
    }

    @Override
    public UserDto getUserByRelationId(Long relationId) {
        User user = userMapper.getUserByRelationId(relationId);
        return convertToDto(user);
    }

    @Override
    public UserDto getUserBySpecialId(Long specialId) {
        User user = userMapper.getUserBySpecialId(specialId);
        return convertToDto(user);
    }

    @Override
    public UserDto getUserByTbUserId(String tbUserId) {
        User user = userMapper.getUserByTbUserId(tbUserId);
        return convertToDto(user);
    }

    @Override
    public UserDto getUserByUnionId(String unionId) {
        User user = userMapper.getUserByUnionId(unionId);
        return convertToDto(user);
    }

    @Override
    public UserDto getUserByPddPid(String pddPid) {
        if (!org.springframework.util.StringUtils.hasText(pddPid)) {
            return null;
        }
        User user = userMapper.getUserByPddPid(pddPid);
        return convertToDto(user);
    }

    @Override
    public UserDto getUserByJdAuthId(String jdAuthId) {
        if (!org.springframework.util.StringUtils.hasText(jdAuthId)) {
            return null;
        }
        User user = userMapper.getUserByJdAuthId(jdAuthId);
        return convertToDto(user);
    }

    @Override
    public UserDto getUserByPhone(String phone) {
        User user = userMapper.getUserByPhone(phone);
        return convertToDto(user);
    }

    @Override
    @Transactional
    public int updateUser(User user) {
        user.setUpdateTime(new Date());
        return userMapper.updateById(user);
    }

    @Override
    @Transactional
    public int updatePddPid(Long userId, String pddPid) {
        User user = new User();
        user.setId(userId);
        user.setPddPid(pddPid);
        return userMapper.updateById(user);
    }

    @Override
    public int updatePddStatus(Long userId, Boolean status) {
        User user = new User();
        user.setId(userId);
        user.setPddStatus(status);
        return userMapper.updateById(user);
    }

    @Override
    @Transactional
    public boolean bindCps(Long userId, String platform, String authId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("USER_NOT_FOUND", "用户不存在");
        }

        switch (platform.toLowerCase()) {
            case "tb":
                // 淘宝通常绑定 relationId 或 specialId
                // 这里简单假设 authId 是纯数字 String，实际可能是 relationId
                try {
                    user.setRelationId(Long.parseLong(authId));
                    // user.setSpecialId(...); // 视具体业务逻辑而定
                } catch (NumberFormatException e) {
                    throw new BusinessException("INVALID_PARAM", "淘宝授权ID格式错误");
                }
                break;
            case "pdd":
                user.setPddPid(authId);
                user.setPddStatus(true);
                break;
            case "jd":
                // 京东：使用独立字段存储（避免与 TB specialId 冲突）
                user.setJdAuthId(authId);
                user.setJdStatus(true);
                break;
            default:
                throw new BusinessException("INVALID_PLATFORM", "不支持的平台");
        }

        return userMapper.updateById(user) > 0;
    }

    @Override
    public UserDto loginByWechat(String openId, String unionId, String sessionKey) {
        // 1. 优先通过UnionID查询 (如果存在)
        User user = null;
        if (unionId != null) {
            user = userMapper.getUserByUnionId(unionId);
        }

        // 2. 如果没找到，通过OpenID查询
        if (user == null) {
            user = userMapper.getUserByMnOpenId(openId);
        }

        // 3. 如果还是没找到，创建新用户
        if (user == null) {
            user = new User();
            user.setMnOpenId(openId);
            user.setUnionId(unionId);
            user.setStatus(1); // 设置状态为正常
            // 默认头像（以及手机号相关字段若未来补齐 phone 后可再更新）
            applyDefaultsForInsert(user);
            userMapper.insert(user);

            // 初始化资金账户
            // moneyService.createWallet(user.getId()); // 假设有这个方法
        } else {
            // 更新OpenID/UnionID (如果缺失)
            boolean update = false;
            if (user.getMnOpenId() == null) {
                user.setMnOpenId(openId);
                update = true;
            }
            if (user.getUnionId() == null && unionId != null) {
                user.setUnionId(unionId);
                update = true;
            }
            if (update) {
                user.setUpdateTime(new Date());
                userMapper.updateById(user);
            }
        }

        // 兜底：对历史用户补齐默认头像/手机号 md5/昵称（不覆盖用户已有设置）
        applyDefaultsForInsert(user);

        // 4. 生成Token (这里简单使用SessionKey作为Token，实际应生成JWT)
        // user.setToken(sessionKey);
        // userMapper.updateById(user);

        // 4. 生成 JWT 并回写
        final String token = jwtService.issueToken(user.getId(), Map.of(
                "phone", user.getPhone() == null ? "" : user.getPhone()
        ));
        user.setToken(token);
        user.setUpdateTime(new Date());
        userMapper.updateById(user);

        return convertToDto(user, token);
    }

    @Override
    public UserDto loginByPassword(String phone, String password) {
        final User user = userMapper.getUserByPhone(phone);
        if (user == null) {
            throw new BusinessException("USER_NOT_FOUND", "用户不存在");
        }
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new BusinessException("NO_PASSWORD", "该账号未设置密码，请使用短信登录");
        }
        if (!encoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException("PASSWORD_INVALID", "密码错误");
        }

        // 兜底：补齐默认字段（不覆盖用户已有设置）
        applyDefaultsForInsert(user);

        final String token = jwtService.issueToken(user.getId(), Map.of(
                "phone", user.getPhone() == null ? "" : user.getPhone()
        ));
        user.setToken(token);
        // 登录态更新也应推进 update_time
        user.setUpdateTime(new Date());
        userMapper.updateById(user);
        return convertToDto(user, token);
    }

    @Override
    @Transactional
    public UserDto loginBySms(String phone, String code) {
        smsCodeService.verifyOrThrow(phone, code);
        User user = userMapper.getUserByPhone(phone);
        if (user == null) {
            // 自动注册（短信登录即注册）
            user = new User();
            user.setPhone(phone);
            user.setStatus(1);
            applyDefaultsForInsert(user);
            userMapper.insert(user);
        }

        // 兜底：历史用户首次短信登录时补齐默认字段
        applyDefaultsForInsert(user);

        final String token = jwtService.issueToken(user.getId(), Map.of(
                "phone", user.getPhone() == null ? "" : user.getPhone()
        ));
        user.setToken(token);
        user.setUpdateTime(new Date());
        userMapper.updateById(user);
        return convertToDto(user, token);
    }

    @Override
    @Transactional
    public UserDto registerBySms(String phone, String code, String password) {
        smsCodeService.verifyOrThrow(phone, code);

        final User existing = userMapper.getUserByPhone(phone);
        if (existing != null) {
            throw new BusinessException("USER_EXISTS", "手机号已注册，请直接登录");
        }

        final User user = new User();
        user.setPhone(phone);
        user.setStatus(1);
        user.setPasswordHash(encoder.encode(password));
        applyDefaultsForInsert(user);
        userMapper.insert(user);

        final String token = jwtService.issueToken(user.getId(), Map.of(
                "phone", user.getPhone() == null ? "" : user.getPhone()
        ));
        user.setToken(token);
        user.setUpdateTime(new Date());
        userMapper.updateById(user);
        return convertToDto(user, token);
    }

    @Override
    @Transactional
    public UserDto bindPhone(Long userId, String phone, String code) {
        final String p = phone == null ? "" : phone.trim();
        if (p.isEmpty()) {
            throw new BusinessException("INVALID_PARAM", "手机号不能为空");
        }
        smsCodeService.verifyOrThrow(p, code);

        final User me = userMapper.selectById(userId);
        if (me == null) {
            throw new BusinessException("USER_NOT_FOUND", "用户不存在");
        }

        // 已绑定且一致：直接返回（并补齐默认字段）
        if (org.springframework.util.StringUtils.hasText(me.getPhone())
                && me.getPhone().trim().equals(p)) {
            applyDefaultsForInsert(me);
            final String token = jwtService.issueToken(me.getId(), Map.of("phone", p));
            me.setToken(token);
            me.setUpdateTime(new Date());
            userMapper.updateById(me);
            return convertToDto(me, token);
        }

        // 该手机号是否已被其他账号占用
        final User existing = userMapper.getUserByPhone(p);
        if (existing != null && (me.getId() == null || !me.getId().equals(existing.getId()))) {
            throw new BusinessException("PHONE_USED", "该手机号已绑定其他账号");
        }

        me.setPhone(p);
        applyDefaultsForInsert(me); // 会按手机号补昵称（若为空），并补齐默认头像

        // 绑定后重签 token（把 phone 写入 claim），并回写到 DB
        final String token = jwtService.issueToken(me.getId(), Map.of("phone", p));
        me.setToken(token);
        me.setUpdateTime(new Date());
        userMapper.updateById(me);
        return convertToDto(me, token);
    }

    @Override
    public UserDto getUserByToken(String token) {
        final String t = token == null ? "" : token.trim();
        if (t.isEmpty()) {
            throw new BusinessException("UNAUTHORIZED", "未登录");
        }
        final User user = userMapper.getUserByToken(t);
        if (user == null) {
            throw new BusinessException("UNAUTHORIZED", "登录已过期");
        }
        return convertToDto(user, t);
    }

    private UserDto convertToDto(User user, String token) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setPhone(user.getPhone());
        dto.setEmail(user.getEmail());
        dto.setRelationId(user.getRelationId());
        dto.setSpecialId(user.getSpecialId());
        dto.setTbUserId(user.getTbUserId());
        dto.setPddPid(user.getPddPid());
        dto.setJdAuthId(user.getJdAuthId());
        dto.setUnionId(user.getUnionId());
        dto.setMpStatus(user.getMpStatus());
        dto.setPddStatus(user.getPddStatus());
        dto.setJdStatus(user.getJdStatus());
        dto.setStatus(user.getStatus());
        dto.setUserDiscount(user.getUserDiscount());
        dto.setTotalActualFee(user.getTotalActualFee());
        dto.setFrozenFee(user.getFrozenFee());
        dto.setToken(token);
        return dto;
    }

    private UserDto convertToDto(User user) {
        return convertToDto(user, user.getToken());
    }

    /**
     * 注册/初始化用户默认字段（仅在字段为空时补齐）。
     *
     * 需求约束：
     * 1) 默认昵称：小桔- + 手机号后四位
     * 2) create_time / update_time：由 DB DEFAULT + 业务更新时推进 update_time
     * 3) 默认头像：使用 Logo（这里用 asset scheme，App 侧可直接渲染本地资源）
     */
    private void applyDefaultsForInsert(User user) {
        if (user == null) return;

        // 头像：无论是否有手机号，都给一个默认值，确保数据库记录有头像
        if (!org.springframework.util.StringUtils.hasText(user.getAvatarUrl())) {
            user.setAvatarUrl("asset:assets/branding/app_icon.png");
        }

        final String phone = user.getPhone();
        if (!org.springframework.util.StringUtils.hasText(phone)) {
            return;
        }
        final String p = phone.trim();

        // nickname：小桔- + 后四位
        if (!org.springframework.util.StringUtils.hasText(user.getNickname())) {
            final String last4 = p.length() >= 4 ? p.substring(p.length() - 4) : p;
            user.setNickname("小桔-" + last4);
        }
    }
}
