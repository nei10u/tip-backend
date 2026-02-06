package com.nei10u.tip.vo;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class HomeLayoutConfigVO {
    private List<HomeLayoutSectionVO> sections;
    private Long updatedAtEpochMillis;

    public static HomeLayoutConfigVO of(List<HomeLayoutSectionVO> sections) {
        HomeLayoutConfigVO vo = new HomeLayoutConfigVO();
        vo.setSections(sections);
        vo.setUpdatedAtEpochMillis(Instant.now().toEpochMilli());
        return vo;
    }
}

