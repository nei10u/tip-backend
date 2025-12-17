package com.nei10u.tip.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nei10u.tip.exception.BusinessException;
import com.nei10u.tip.mapper.WithdrawMapper;
import com.nei10u.tip.model.WithdrawRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 提现服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawService {

    private final WithdrawMapper withdrawMapper;
    private final MoneyService moneyService;

    /**
     * 申请提现
     */
    @Transactional
    public boolean apply(Long userId, Double amount, String account, String type, String realName) {
        log.info("用户 {} 申请提现 {} 元", userId, amount);

        // 1. 冻结资金
        boolean frozen = moneyService.withdraw(String.valueOf(userId), amount);
        if (!frozen) {
            throw new BusinessException("FREEZE_FAILED", "资金冻结失败");
        }

        // 2. 创建提现记录
        WithdrawRecord record = new WithdrawRecord();
        record.setUserId(userId);
        record.setAmount(amount);
        record.setAccount(account);
        record.setType(type);
        record.setRealName(realName);
        record.setStatus(0); // 审核中
        withdrawMapper.insert(record);

        return true;
    }

    /**
     * 审核提现
     * 
     * @param recordId 记录ID
     * @param approved 是否通过
     * @param remark   备注
     */
    @Transactional
    public boolean audit(Long recordId, boolean approved, String remark) {
        WithdrawRecord record = withdrawMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("RECORD_NOT_FOUND", "提现记录不存在");
        }
        if (record.getStatus() != 0) {
            throw new BusinessException("INVALID_STATUS", "该记录已审核");
        }

        if (approved) {
            // 审核通过: 扣除冻结资金
            moneyService.deductFrozenMoney(String.valueOf(record.getUserId()), record.getAmount());
            record.setStatus(1);
        } else {
            // 审核驳回: 解冻资金
            moneyService.unfreezeMoney(String.valueOf(record.getUserId()), record.getAmount());
            record.setStatus(2);
        }

        record.setRemark(remark);
        withdrawMapper.updateById(record);
        return true;
    }

    /**
     * 获取用户提现记录
     */
    public List<WithdrawRecord> getUserRecords(Long userId) {
        return withdrawMapper.getRecordsByUserId(userId);
    }
}
