package com.nei10u.tip.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nei10u.tip.mapper.HomeBannerMapper;
import com.nei10u.tip.mapper.HomeMenuMapper;
import com.nei10u.tip.mapper.HomeNoticeMapper;
import com.nei10u.tip.mapper.HomeSectionMapper;
import com.nei10u.tip.model.HomeBanner;
import com.nei10u.tip.model.HomeMenu;
import com.nei10u.tip.model.HomeNotice;
import com.nei10u.tip.model.HomeSection;
import com.nei10u.tip.service.HomeService;
import com.nei10u.tip.vo.HomeConfigVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 首页业务服务实现类
 */
@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private final HomeBannerMapper bannerMapper;
    private final HomeMenuMapper menuMapper;
    private final HomeSectionMapper sectionMapper;
    private final HomeNoticeMapper noticeMapper;

    /**
     * 实现获取首页配置的逻辑
     */
    @Override
    public HomeConfigVO getHomeConfig() {
        HomeConfigVO vo = new HomeConfigVO();

        // 1. 查询轮播图
        List<HomeBanner> banners = bannerMapper.selectList(
                new LambdaQueryWrapper<HomeBanner>()
                        .eq(HomeBanner::getStatus, 1)
                        .orderByAsc(HomeBanner::getSortOrder));
        vo.setBanners(banners);

        // 2. 查询菜单
        List<HomeMenu> menus = menuMapper.selectList(
                new LambdaQueryWrapper<HomeMenu>()
                        .eq(HomeMenu::getStatus, 1)
                        .orderByAsc(HomeMenu::getSortOrder));
        vo.setMenus(menus);

        // 3. 查询活动专区
        List<HomeSection> sections = sectionMapper.selectList(
                new LambdaQueryWrapper<HomeSection>()
                        .eq(HomeSection::getStatus, 1)
                        .orderByAsc(HomeSection::getSortOrder));
        vo.setSections(sections);
        
        // 4. 查询公告 (跑马灯)
        List<HomeNotice> notices = noticeMapper.selectList(
                new LambdaQueryWrapper<HomeNotice>()
                        .eq(HomeNotice::getStatus, 1)
                        .orderByAsc(HomeNotice::getSortOrder));
        vo.setNotices(notices);

        return vo;
    }
}
