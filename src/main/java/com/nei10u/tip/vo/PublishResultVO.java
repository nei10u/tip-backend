package com.nei10u.tip.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PublishResultVO {
    private List<String> requested = new ArrayList<>();
    private List<String> refreshed = new ArrayList<>();
    private List<String> skipped = new ArrayList<>();
}

