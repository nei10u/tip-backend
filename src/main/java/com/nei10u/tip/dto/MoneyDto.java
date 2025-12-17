package com.nei10u.tip.dto;

import lombok.Data;

/**
 * 资金DTO
 */
@Data
public class MoneyDto {

    private Long id;
    private String userId;

    private Double balance;
    private Double frozen;
    private Double totalIncome;
    private Double totalWithdraw;
}
