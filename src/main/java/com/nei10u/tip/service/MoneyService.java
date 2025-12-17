package com.nei10u.tip.service;

import com.nei10u.tip.dto.MoneyDto;

/**
 * 资金服务接口
 */
public interface MoneyService {

    /**
     * 根据用户ID获取资金信息
     */
    MoneyDto getMoneyByUserId(String userId);

    /**
     * 更新余额
     */
    int updateBalance(String userId, Double amount);

    /**
     * 冻结金额
     */
    int freezeMoney(String userId, Double amount);

    /**
     * 解冻金额 (提现失败/驳回)
     */
    int unfreezeMoney(String userId, Double amount);

    /**
     * 扣除冻结金额 (提现成功)
     */
    int deductFrozenMoney(String userId, Double amount);

    /**
     * 申请提现 (冻结金额)
     */
    boolean withdraw(String userId, Double amount);
}
