package com.nei10u.tip.service.impl;

import com.nei10u.tip.dto.MoneyDto;
import com.nei10u.tip.exception.BusinessException;
import com.nei10u.tip.mapper.MoneyMapper;
import com.nei10u.tip.model.Money;
import com.nei10u.tip.service.MoneyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 资金服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MoneyServiceImpl implements MoneyService {

    private final MoneyMapper moneyMapper;

    @Override
    public MoneyDto getMoneyByUserId(String userId) {
        Money money = moneyMapper.getMoneyByUserId(userId);
        if (money == null) {
            // 如果用户没有资金记录，创建一个
            money = new Money();
            money.setUserId(userId);
            money.setBalance(0.0);
            money.setFrozen(0.0);
            money.setTotalIncome(0.0);
            money.setTotalWithdraw(0.0);
            moneyMapper.insert(money);
        }
        return convertToDto(money);
    }

    @Override
    @Transactional
    public int updateBalance(String userId, Double amount) {
        return moneyMapper.updateBalance(userId, amount);
    }

    @Override
    @Transactional
    public int freezeMoney(String userId, Double amount) {
        Money money = moneyMapper.getMoneyByUserId(userId);
        if (money == null || money.getBalance() < amount) {
            throw new BusinessException("INSUFFICIENT_BALANCE", "余额不足");
        }
        return moneyMapper.freezeMoney(userId, amount);
    }

    @Override
    @Transactional
    public int unfreezeMoney(String userId, Double amount) {
        return moneyMapper.unfreezeMoney(userId, amount);
    }

    @Override
    @Transactional
    public int deductFrozenMoney(String userId, Double amount) {
        return moneyMapper.deductFrozenMoney(userId, amount);
    }

    @Override
    @Transactional
    public boolean withdraw(String userId, Double amount) {
        Money money = moneyMapper.getMoneyByUserId(userId);
        if (money == null || money.getBalance() < amount) {
            throw new BusinessException("INSUFFICIENT_BALANCE", "余额不足");
        }

        // 冻结金额
        freezeMoney(userId, amount);

        // TODO: 调用提现接口
        log.info("用户 {} 申请提现 {} 元", userId, amount);

        return true;
    }

    private MoneyDto convertToDto(Money money) {
        if (money == null) {
            return null;
        }
        MoneyDto dto = new MoneyDto();
        BeanUtils.copyProperties(money, dto);
        return dto;
    }
}
