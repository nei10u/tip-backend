package com.nei10u.tip.goods.normalize;

import com.alibaba.fastjson2.JSONObject;

/**
 * 将第三方/平台返回的原始 JSON 归一化为 Flutter 可消费的 Goods 列表结构。
 *
 * 统一输出约定（建议）：
 * - list: 标准商品数组（字段命名对齐 tip_flutter_app 的 Goods.fromJson）
 * - raw: 原始响应（便于排查）
 * - pageId/pageSize: 回显分页参数（可选）
 */
public interface GoodsNormalizer {
    GoodsNormalizeType type();

    JSONObject normalize(JSONObject raw, int pageId, int pageSize);
}


