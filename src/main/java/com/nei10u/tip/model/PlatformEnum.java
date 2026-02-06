package com.nei10u.tip.model;

import lombok.Getter;

@Getter
public enum PlatformEnum {
    TB("TB", "淘宝"),
    JD("JD", "京东");

    private final String code;
    private final String name;

    PlatformEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
