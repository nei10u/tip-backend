package com.nei10u.tip.controller;

import com.nei10u.tip.dto.MoneyDto;
import com.nei10u.tip.service.MoneyService;
import com.nei10u.tip.vo.ResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 资金控制器
 */
@Tag(name = "资金接口")
@RestController
@RequestMapping("/api/money")
@RequiredArgsConstructor
public class MoneyController {

    private final MoneyService moneyService;

    @Operation(summary = "获取用户余额")
    @GetMapping("/{userId}")
    public ResponseVO<MoneyDto> getMoneyByUserId(@PathVariable String userId) {
        MoneyDto money = moneyService.getMoneyByUserId(userId);
        return ResponseVO.success(money);
    }
}
