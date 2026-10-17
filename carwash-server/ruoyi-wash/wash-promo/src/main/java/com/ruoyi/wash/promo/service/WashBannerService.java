package com.ruoyi.wash.promo.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.promo.domain.WashBanner;
import com.ruoyi.wash.promo.dto.BannerVO;
import com.ruoyi.wash.promo.mapper.WashBannerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页 Banner：C 端取生效轮播 + 后台增删改查。
 *
 * <p>契约：openapi.yaml /api/v1/banners、/admin-api/wash/banner/**。
 */
@Service
public class WashBannerService {

    private static final String SCENE_HOME = "HOME";
    private static final List<String> LINK_TYPES = List.of("NONE", "ACTIVITY", "RECHARGE", "SERVICE");

    @Autowired
    private WashBannerMapper bannerMapper;

    /** C 端：首页轮播（上架 + 生效窗口内） */
    public List<BannerVO> homeBanners() {
        List<BannerVO> list = new ArrayList<>();
        for (WashBanner b : bannerMapper.selectActive(SCENE_HOME, System.currentTimeMillis())) {
            list.add(toVO(b));
        }
        return list;
    }

    // ---------------- 后台 ----------------

    public Map<String, Object> page(String title, int pageNum, int pageSize) {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 10;
        int offset = (pageNum - 1) * pageSize;
        Map<String, Object> data = new HashMap<>();
        data.put("rows", bannerMapper.selectAdminList(title, offset, pageSize));
        data.put("total", bannerMapper.countAdmin(title));
        return data;
    }

    public WashBanner get(Long bannerId) {
        WashBanner banner = bannerMapper.selectById(bannerId);
        if (banner == null) {
            throw new ApiException(ErrorCode.A0001, "Banner 不存在");
        }
        return banner;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long add(WashBanner banner) {
        normalize(banner);
        bannerMapper.insert(banner);
        return banner.getBannerId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void edit(WashBanner banner) {
        if (banner.getBannerId() == null) {
            throw new ApiException(ErrorCode.A0001, "bannerId 必填");
        }
        normalize(banner);
        if (bannerMapper.update(banner) == 0) {
            throw new ApiException(ErrorCode.A0001, "Banner 不存在或已删除");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(Long bannerId) {
        if (bannerMapper.logicDelete(bannerId) == 0) {
            throw new ApiException(ErrorCode.A0001, "Banner 不存在或已删除");
        }
    }

    /** 归一化：场景/跳转类型/排序/上下架的兜底与校验 */
    private void normalize(WashBanner banner) {
        if (banner.getTitle() == null || banner.getTitle().isBlank()) {
            throw new ApiException(ErrorCode.A0001, "Banner 标题不能为空");
        }
        if (banner.getScene() == null || banner.getScene().isBlank()) {
            banner.setScene(SCENE_HOME);
        }
        if (!SCENE_HOME.equals(banner.getScene())) {
            throw new ApiException(ErrorCode.A0001, "暂只支持首页场景（HOME）");
        }
        if (banner.getLinkType() == null || !LINK_TYPES.contains(banner.getLinkType())) {
            banner.setLinkType("NONE");
        }
        if (banner.getEnabled() == null || (!"Y".equals(banner.getEnabled()) && !"N".equals(banner.getEnabled()))) {
            banner.setEnabled("Y");
        }
        if (banner.getSortOrder() == null) {
            banner.setSortOrder(0);
        }
        if (banner.getImageUrl() != null && banner.getImageUrl().isBlank()) {
            banner.setImageUrl(null);
        }
        if (banner.getSubtitle() != null && banner.getSubtitle().isBlank()) {
            banner.setSubtitle(null);
        }
    }

    private BannerVO toVO(WashBanner b) {
        BannerVO vo = new BannerVO();
        vo.setBannerId(b.getBannerId());
        vo.setImageUrl(b.getImageUrl());
        vo.setTitle(b.getTitle());
        vo.setSubtitle(b.getSubtitle());
        vo.setLinkType(b.getLinkType());
        vo.setLinkValue(b.getLinkValue());
        vo.setSort(b.getSortOrder());
        return vo;
    }
}
