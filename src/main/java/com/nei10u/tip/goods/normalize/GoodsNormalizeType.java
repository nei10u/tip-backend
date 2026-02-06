package com.nei10u.tip.goods.normalize;

/**
 * 统一 Goods 列表的“来源类型”。
 *
 * 后续接入其他平台/第三方接口时，只需新增一个 normalizer 实现即可。
 */
public enum GoodsNormalizeType {
    /** 折淘客：实时人气榜 api_shishi.ashx */
    ZTK_SHISHI,

    /** 好京客：唯品会商品查询 vip/goodsquery */
    HJK_VIP,
}
