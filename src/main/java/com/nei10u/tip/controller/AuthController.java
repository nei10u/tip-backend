package com.nei10u.tip.controller;

import com.nei10u.tip.tb.TbAuthService;
import com.nei10u.tip.vo.ResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 多平台授权/绑定接口：
 * - start：生成 state 并返回授权 URL
 * - callback：回调（code/state）处理绑定
 * - result：前端轮询授权结果
 */
@Tag(name = "多平台授权接口")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final TbAuthService tbAuthService;

    @Operation(summary = "发起淘宝授权（返回 authUrl + state）")
    @PostMapping("/tb/auth/start")
    public ResponseVO<Map<String, Object>> start(@RequestParam Long userId) {
        return ResponseVO.success(tbAuthService.startAuth(userId));
    }

    @Operation(summary = "查询淘宝授权结果（轮询）")
    @GetMapping("/tb/auth/result")
    public ResponseVO<Map<String, Object>> result(@RequestParam String state) {
        return ResponseVO.success(tbAuthService.getResult(state));
    }

    @Operation(summary = "解除淘宝绑定（清空 tbUserId/specialId/relationId）")
    @PostMapping("/tb/auth/unbind")
    public ResponseVO<Boolean> unbind(@RequestParam Long userId) {
        tbAuthService.unbind(userId);
        return ResponseVO.success(true);
    }

    @Operation(summary = "淘宝 OAuth 回调（H5）")
    @GetMapping(value = "/tb/oauth/callback", produces = MediaType.TEXT_HTML_VALUE)
    public String callback(@RequestParam(required = false) String code,
                           @RequestParam(required = false) String state) {
        try {
            tbAuthService.handleCallback(code, state);
            return renderHtml("授权成功", "你已完成淘宝授权，可以返回 App 继续使用。");
        } catch (Exception e) {
            // handleCallback 内部会写入 result，前端轮询可拿到失败原因
            return renderHtml("授权失败", e.getMessage() == null ? "授权失败" : e.getMessage());
        }
    }

    private static String renderHtml(String title, String msg) {
        // 简易 H5：避免前端必须引入 WebView
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="utf-8"/>
                  <meta name="viewport" content="width=device-width, initial-scale=1"/>
                  <title>%s</title>
                  <style>
                    body{font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial;
                         margin:0;padding:24px;background:#f7f7f7;color:#111;}
                    .card{background:#fff;border-radius:14px;padding:20px;box-shadow:0 6px 24px rgba(0,0,0,.06);}
                    h1{font-size:18px;margin:0 0 10px;}
                    p{margin:0;color:#444;line-height:1.5;}
                  </style>
                </head>
                <body>
                  <div class="card">
                    <h1>%s</h1>
                    <p>%s</p>
                  </div>
                </body>
                </html>
                """.formatted(title, title, msg);
    }
}


