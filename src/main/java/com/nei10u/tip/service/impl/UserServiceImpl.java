package com.nei10u.tip.service.impl;

import com.nei10u.tip.dto.UserDto;
import com.nei10u.tip.exception.BusinessException;
import com.nei10u.tip.mapper.UserMapper;
import com.nei10u.tip.model.User;
import com.nei10u.tip.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

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
    public UserDto getUserByPhone(String phone) {
        User user = userMapper.getUserByPhone(phone);
        return convertToDto(user);
    }

    @Override
    @Transactional
    public int updateUser(User user) {
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
                // 京东逻辑待定，暂存 specialId 字段或新增字段
                try {
                    user.setSpecialId(Long.parseLong(authId));
                } catch (NumberFormatException e) {
                    // ignore or throw
                }
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
                userMapper.updateById(user);
            }
        }

        // 4. 生成Token (这里简单使用SessionKey作为Token，实际应生成JWT)
        // user.setToken(sessionKey);
        // userMapper.updateById(user);

        return convertToDto(user);
    }

    private UserDto convertToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setNickname(user.getNickname());
        dto.setPhone(user.getPhone());
        dto.setRelationId(user.getRelationId());
        dto.setSpecialId(user.getSpecialId());
        dto.setTbUserId(user.getTbUserId());
        dto.setPddPid(user.getPddPid());
        dto.setUnionId(user.getUnionId());
        return dto;
    }
}
