package com.nei10u.tip.service;

import com.alibaba.fastjson2.JSONObject;
import com.nei10u.tip.service.impl.DtkGoodsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DtkDtkGoodsServiceTest {

    @Mock
    private DtkApiService dtkApiService;

    @InjectMocks
    private DtkGoodsServiceImpl goodsService;

    @Test
    public void testConvertLink_Tb() {
        String mockResponse = "{\"code\":0,\"data\":{\"click_url\":\"https://s.click.taobao.com/xxx\"}}";
        // DtkGoodsServiceImpl.convertLink 当前未传 pid，使用 null
        when(dtkApiService.getPrivilegeLink(eq("123456"), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(mockResponse);

        JSONObject result = goodsService.convertLink("tb", "123456", "user1");

        assertNotNull(result);
        assertEquals(0, result.getIntValue("code"));
        assertNotNull(result.getJSONObject("data"));
    }
}
