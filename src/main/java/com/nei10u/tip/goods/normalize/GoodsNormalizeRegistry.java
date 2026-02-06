package com.nei10u.tip.goods.normalize;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Normalizer 注册表：按 type 选择对应实现。
 */
@Component
public class GoodsNormalizeRegistry {
    private final Map<GoodsNormalizeType, GoodsNormalizer> map = new EnumMap<>(GoodsNormalizeType.class);

    public GoodsNormalizeRegistry(List<GoodsNormalizer> normalizers) {
        for (GoodsNormalizer n : normalizers) {
            map.put(n.type(), n);
        }
    }

    public GoodsNormalizer get(GoodsNormalizeType type) {
        GoodsNormalizer n = map.get(type);
        if (n == null) {
            throw new IllegalArgumentException("No GoodsNormalizer registered for type=" + type);
        }
        return n;
    }
}


