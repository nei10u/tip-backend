package com.nei10u.tip.tb;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nei10u.tip.exception.BusinessException;
import com.nei10u.tip.mapper.UserMapper;
import com.nei10u.tip.model.User;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
import com.taobao.api.request.TbkScPublisherInfoSaveRequest;
import com.taobao.api.request.TopAuthTokenCreateRequest;
import com.taobao.api.response.TbkScPublisherInfoSaveResponse;
import com.taobao.api.response.TopAuthTokenCreateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 淘宝 OAuth 授权/绑定服务：
 * - startAuth: 生成 state 并写入 Redis，返回淘宝授权 URL
 * - handleCallback: 用 code 换 tokenResult，并落库 specialId/relationId/tbUserId
 * - getResult: 前端轮询授权结果
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TbAuthService {

    private final TbProperties tbProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserMapper userMapper;

    private String stateKey(String state) {
        return "tb:oauth:state:" + state;
    }

    private String resultKey(String state) {
        return "tb:oauth:result:" + state;
    }

    public Map<String, Object> startAuth(Long userId) {
        if (userId == null) {
            throw new BusinessException("INVALID_PARAM", "userId不能为空");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("USER_NOT_FOUND", "用户不存在");
        }

        if (!StringUtils.hasText(tbProperties.getAppKey()) || !StringUtils.hasText(tbProperties.getAppSecret())) {
            throw new BusinessException("TB_CONFIG_MISSING", "淘宝开放平台配置缺失(app-key/app-secret)");
        }
        if (tbProperties.getOauth() == null || !StringUtils.hasText(tbProperties.getOauth().getRedirectUri())) {
            throw new BusinessException("TB_CONFIG_MISSING", "淘宝授权回调地址缺失(redirect-uri)");
        }

        String state = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(
                stateKey(state),
                String.valueOf(userId),
                Duration.ofSeconds(tbProperties.getOauth().getStateTtlSeconds())
        );

        // 构造淘宝授权地址
        // https://oauth.taobao.com/authorize?response_type=code&client_id=APPKEY&redirect_uri=...&state=...&view=wap
        String redirectUri = tbProperties.getOauth().getRedirectUri();
        String authUrl = UriComponentsBuilder
                .fromHttpUrl(tbProperties.getOauth().getAuthorizeUrl())
                .queryParam("response_type", "code")
                .queryParam("client_id", tbProperties.getAppKey())
                .queryParam("redirect_uri", URLEncoder.encode(redirectUri, StandardCharsets.UTF_8))
                .queryParam("state", state)
                // view=wap：更适配移动端；如需 PC 可移除
                .queryParam("view", "wap")
                .build(true)
                .toUriString();

        Map<String, Object> data = new HashMap<>();
        data.put("state", state);
        data.put("authUrl", authUrl);
        data.put("stateTtlSeconds", tbProperties.getOauth().getStateTtlSeconds());
        return data;
    }

    @Transactional
    public void unbind(Long userId) {
        if (userId == null) {
            throw new BusinessException("INVALID_PARAM", "userId不能为空");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("USER_NOT_FOUND", "用户不存在");
        }

        LambdaUpdateWrapper<User> uw = new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getTbUserId, null)
                .set(User::getRelationId, null)
                .set(User::getSpecialId, null);
        userMapper.update(null, uw);
    }

    public Map<String, Object> getResult(String state) {
        if (!StringUtils.hasText(state)) {
            throw new BusinessException("INVALID_PARAM", "state不能为空");
        }

        Object r = redisTemplate.opsForValue().get(resultKey(state));
        if (r instanceof Map<?, ?> rm) {
            Map<String, Object> out = new HashMap<>();
            rm.forEach((k, v) -> {
                if (k != null) out.put(String.valueOf(k), v);
            });
            return out;
        }

        Boolean stateExists = redisTemplate.hasKey(stateKey(state));
        if (Boolean.TRUE.equals(stateExists)) {
            return Map.of("status", "PENDING", "message", "等待用户完成授权");
        }

        return Map.of("status", "EXPIRED", "message", "授权已过期，请重新发起授权");
    }

    @Transactional
    public void handleCallback(String code, String state) {
        if (!StringUtils.hasText(code) || !StringUtils.hasText(state)) {
            saveResult(state, "FAIL", "授权失败：缺少 code/state");
            throw new BusinessException("INVALID_PARAM", "缺少 code/state");
        }

        Object userIdStr = redisTemplate.opsForValue().get(stateKey(state));
        if (userIdStr == null) {
            saveResult(state, "EXPIRED", "授权已过期，请重新发起授权");
            throw new BusinessException("STATE_EXPIRED", "state已过期或无效");
        }

        Long userId;
        try {
            userId = Long.parseLong(String.valueOf(userIdStr));
        } catch (Exception e) {
            saveResult(state, "FAIL", "授权失败：state绑定数据异常");
            throw new BusinessException("STATE_INVALID", "state绑定数据异常");
        }

        TaobaoClient client = new DefaultTaobaoClient(tbProperties.getGateway(), tbProperties.getAppKey(), tbProperties.getAppSecret());

        String tbUserId;
        String accessToken;
        try {
            TopAuthTokenCreateRequest req = new TopAuthTokenCreateRequest();
            req.setCode(code);
            TopAuthTokenCreateResponse rsp = client.execute(req);
            String tokenResult = rsp.getTokenResult();
            if (!StringUtils.hasText(tokenResult)) {
                saveResult(state, "FAIL", "授权失败：tokenResult为空");
                throw new BusinessException("TB_AUTH_FAILED", "授权失败[tokenResult为空]");
            }

            JSONObject json = JSONObject.parseObject(tokenResult);
            tbUserId = json.getString("taobao_user_id");
            accessToken = json.getString("access_token");

            if (!StringUtils.hasText(tbUserId) || !StringUtils.hasText(accessToken)) {
                saveResult(state, "FAIL", "授权失败：tokenResult字段缺失");
                throw new BusinessException("TB_AUTH_FAILED", "授权失败[tokenResult字段缺失]");
            }
        } catch (Exception e) {
            log.error("tb oauth exchange failed: state={}", state, e);
            saveResult(state, "FAIL", "授权失败：换取 access_token 失败");
            throw (e instanceof BusinessException) ? (BusinessException) e : new BusinessException("TB_AUTH_FAILED", "授权失败：换取 access_token 失败");
        }

        // 绑定一致性校验
        User current = userMapper.selectById(userId);
        if (current == null) {
            saveResult(state, "FAIL", "授权失败：用户不存在");
            throw new BusinessException("USER_NOT_FOUND", "用户不存在");
        }

        if (StringUtils.hasText(current.getTbUserId()) && !current.getTbUserId().equals(tbUserId)) {
            saveResult(state, "FAIL", "两次绑定淘宝账号不一致");
            throw new BusinessException("TB_BIND_MISMATCH", "两次绑定淘宝账号不一致");
        }

        User other = userMapper.getUserByTbUserId(tbUserId);
        if (other != null && other.getId() != null && !other.getId().equals(userId)) {
            saveResult(state, "FAIL", "该淘宝账号已绑定其他账号");
            throw new BusinessException("TB_ALREADY_BOUND", "该淘宝账号已绑定其他账号");
        }

        // 调用 publisherInfo.save 获取 specialId/relationId
        Long specialId = null;
        Long relationId = null;
        try {
            TbkScPublisherInfoSaveRequest reqSave = new TbkScPublisherInfoSaveRequest();
            if (StringUtils.hasText(tbProperties.getOauth().getInviterCode())) {
                reqSave.setInviterCode(tbProperties.getOauth().getInviterCode());
            }
            if (tbProperties.getOauth().getInfoType() != null) {
                reqSave.setInfoType(tbProperties.getOauth().getInfoType());
            }
            if (StringUtils.hasText(tbProperties.getOauth().getNote())) {
                reqSave.setNote(tbProperties.getOauth().getNote());
            }

            TbkScPublisherInfoSaveResponse rspSave = client.execute(reqSave, accessToken);
            String errCode = rspSave.getErrorCode();
            if (StringUtils.hasText(errCode) && !"0".equals(errCode)) {
                // 淘宝返回非 0 时，尽量把 subMsg 带回去方便定位
                String subMsg = rspSave.getSubMsg();
                log.warn("tb publisher save failed: errCode={}, subMsg={}", errCode, subMsg);
                saveResult(state, "FAIL", StringUtils.hasText(subMsg) ? subMsg : "授权失败：渠道信息获取失败");
                throw new BusinessException("TB_PUBLISHER_SAVE_FAILED", StringUtils.hasText(subMsg) ? subMsg : "渠道信息获取失败");
            }

            TbkScPublisherInfoSaveResponse.Data data = rspSave.getData();
            if (data != null) {
                // 方法名以 SDK 实际为准（若编译报错再做适配）
                specialId = data.getSpecialId();
                relationId = data.getRelationId();
            }
        } catch (Exception e) {
            log.error("tb publisherInfo.save failed: state={}", state, e);
            if (!(e instanceof BusinessException)) {
                saveResult(state, "FAIL", "授权失败：获取渠道ID失败");
            }
            throw (e instanceof BusinessException) ? (BusinessException) e : new BusinessException("TB_PUBLISHER_SAVE_FAILED", "获取渠道ID失败");
        }

        // 落库
        User upd = new User();
        upd.setId(userId);
        upd.setTbUserId(tbUserId);
        if (specialId != null) {
            upd.setSpecialId(specialId);
        }
        if (relationId != null) {
            upd.setRelationId(relationId);
        }
        userMapper.updateById(upd);

        // 成功：写结果、清 state
        saveResult(state, "SUCCESS", "授权成功");
        redisTemplate.delete(stateKey(state));
    }

    private void saveResult(String state, String status, String message) {
        if (!StringUtils.hasText(state)) return;
        Map<String, Object> r = new HashMap<>();
        r.put("status", status);
        r.put("message", message);
        redisTemplate.opsForValue().set(
                resultKey(state),
                r,
                Duration.ofSeconds(tbProperties.getOauth().getResultTtlSeconds())
        );
    }
}


