package com.nei10u.tip.controller;

import com.nei10u.tip.model.WithdrawRecord;
import com.nei10u.tip.service.WithdrawService;
import com.nei10u.tip.vo.ResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 提现控制器
 */
@Tag(name = "提现接口")
@RestController
@RequestMapping("/api/withdraw")
@RequiredArgsConstructor
public class WithdrawController {

    private final WithdrawService withdrawService;

    @Operation(summary = "申请提现")
    @PostMapping("/apply")
    public ResponseVO<Boolean> apply(
            @RequestParam Long userId,
            @RequestParam Double amount,
            @RequestParam String account,
            @RequestParam String type,
            @RequestParam String realName) {
        boolean result = withdrawService.apply(userId, amount, account, type, realName);
        return ResponseVO.success(result);
    }

    @Operation(summary = "审核提现")
    @PostMapping("/audit")
    public ResponseVO<Boolean> audit(
            @RequestParam Long recordId,
            @RequestParam boolean approved,
            @RequestParam(required = false) String remark) {
        boolean result = withdrawService.audit(recordId, approved, remark);
        return ResponseVO.success(result);
    }

    @Operation(summary = "获取提现记录")
    @GetMapping("/records")
    public ResponseVO<List<WithdrawRecord>> getUserRecords(@RequestParam Long userId) {
        List<WithdrawRecord> records = withdrawService.getUserRecords(userId);
        return ResponseVO.success(records);
    }
}
