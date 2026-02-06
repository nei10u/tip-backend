package com.nei10u.tip.exception;

import com.nei10u.tip.vo.ResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：把业务异常收敛成统一 ResponseVO，避免前端解析不一致。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseVO<Void> handleBusiness(BusinessException e) {
        // 业务错误统一走 400，message 透出给前端展示
        return ResponseVO.error(400, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseVO<Void> handleAny(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseVO.error(500, "系统异常");
    }
}


