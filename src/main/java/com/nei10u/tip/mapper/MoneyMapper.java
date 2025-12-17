package com.nei10u.tip.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nei10u.tip.model.Money;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 资金Mapper
 */
@Mapper
public interface MoneyMapper extends BaseMapper<Money> {

        /**
         * 根据用户ID查询资金信息
         */
        Money getMoneyByUserId(@Param("userId") String userId);

        /**
         * 更新余额
         */
        int updateBalance(@Param("userId") String userId,
                        @Param("amount") Double amount);

        /**
         * 冻结金额
         */
        int freezeMoney(@Param("userId") String userId,
                        @Param("amount") Double amount);

        /**
         * 解冻金额
         */
        int unfreezeMoney(@Param("userId") String userId,
                        @Param("amount") Double amount);

        /**
         * 扣除冻结金额 (提现成功)
         */
        int deductFrozenMoney(@Param("userId") String userId,
                        @Param("amount") Double amount);
}
