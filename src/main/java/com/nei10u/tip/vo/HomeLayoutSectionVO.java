package com.nei10u.tip.vo;

import lombok.Data;

import java.util.Map;

@Data
public class HomeLayoutSectionVO {
    private Integer sortOrder;
    private String type;
    private Map<String, Object> config;
}

