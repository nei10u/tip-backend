package com.nei10u.tip.service;

import com.alibaba.fastjson2.JSONObject;

/**
 * TB 转链服务（多供应商兜底）。
 *
 * 统一输出字段：
 * - provider: ZTK / VEAPI / DTK
 * - longUrl / shortUrl / tpwd
 * - raw: 原始响应（可选，便于审计与排障）
 */
public interface TbConvertService {

    /**
     * 将商品/链接转换为可归因的推广链接/口令。
     *
     * @param goodsId    商品ID（通常为 num_iid / itemId；也允许传 URL/口令文本，交由下游识别）
     * @param relationId 淘宝渠道关系ID（优先使用；为空则可能导致归因丢失）
     * @param specialId  淘宝 specialId（DTK 转链可能使用；可为空）
     * @param pid        推广位 PID（mm_xxx_xxx_xxx；为空使用默认配置）
     * @param externalId 外部ID（用于追踪/幂等；可为空）
     */
    JSONObject convert(String goodsId, String relationId, String specialId, String pid, String externalId);
}



