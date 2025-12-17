package com.nei10u.tip.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 实名认证服务
 * 目前使用Mock实现，后续需集成云账户SDK
 */
@Slf4j
@Service
public class RealNameService {

    /**
     * 实名认证验证
     * 
     * @param realName 真实姓名
     * @param idCard   身份证号
     * @return 是否验证通过
     */
    public boolean verify(String realName, String idCard) {
        log.info("发起实名认证: name={}, idCard={}", realName, idCard);

        // TODO: 集成云账户SDK进行真实验证
        // 目前仅做简单格式校验模拟
        if (realName == null || realName.length() < 2) {
            return false;
        }
        if (idCard == null || idCard.length() != 18) {
            return false;
        }

        // 模拟验证通过
        return true;
    }
}
