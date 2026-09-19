package com.ruoyi.wash.network.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.network.domain.WashCommunity;
import com.ruoyi.wash.network.domain.WashSite;
import com.ruoyi.wash.network.dto.CommunityVO;
import com.ruoyi.wash.network.dto.SiteVO;
import com.ruoyi.wash.network.mapper.WashCommunityMapper;
import com.ruoyi.wash.network.mapper.WashSiteMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 站点与小区查询。跨模块只能调 Service（订单域用它补全站点名与小区名）。
 *
 * <p>站点是产能维度：产能按「站点 + 日期」计数，小区只决定能不能下单（地库通行授权）。
 */
@Service
public class WashSiteQueryService {

    private static final String YES = "Y";

    @Autowired
    private WashSiteMapper siteMapper;

    @Autowired
    private WashCommunityMapper communityMapper;

    public WashSite require(Long siteId) {
        WashSite site = siteMapper.selectById(siteId);
        if (site == null) {
            throw new ApiException(ErrorCode.A0001, "站点不存在");
        }
        return site;
    }

    /**
     * 当前站点：一个站点可能服务多个小区，展示取该站点下第一个已开通小区
     * （PRD P1 顶部站点栏是「小区 · 站点」的固定文案，MVP 只有一个小区）。
     */
    public SiteVO current(Long siteId) {
        WashSite site = require(siteId);
        List<WashCommunity> communities = communityMapper.selectBySite(siteId);
        WashCommunity community = communities.isEmpty() ? null : communities.get(0);

        SiteVO vo = new SiteVO();
        vo.setSiteId(site.getSiteId());
        vo.setSiteName(site.getSiteName());
        vo.setServiceStatus(site.getServiceStatus());
        vo.setClosedNotice(site.getClosedNotice());
        if (community != null) {
            vo.setCommunityId(community.getCommunityId());
            vo.setCommunityName(community.getCommunityName());
            vo.setDisplayName(community.getCommunityName() + " · " + site.getSiteName());
        } else {
            vo.setDisplayName(site.getSiteName());
        }
        return vo;
    }

    public List<CommunityVO> listCommunities() {
        List<CommunityVO> list = new ArrayList<>();
        for (WashCommunity c : communityMapper.selectEnabledList()) {
            CommunityVO vo = new CommunityVO();
            vo.setCommunityId(c.getCommunityId());
            vo.setName(c.getCommunityName());
            vo.setAccessAuth(YES.equals(c.getAccessAuth()));
            vo.setEnabled(YES.equals(c.getEnabled()));
            list.add(vo);
        }
        return list;
    }

    /** 站点名：订单详情等处补全用，查不到返回 null 而不是抛异常（详情不能因为站点缺数据而整个失败） */
    public String siteName(Long siteId) {
        WashSite site = siteId == null ? null : siteMapper.selectById(siteId);
        return site == null ? null : site.getSiteName();
    }

    /** 小区名：同上，查不到返回 null */
    public String communityName(Long communityId) {
        WashCommunity c = communityId == null ? null : communityMapper.selectById(communityId);
        return c == null ? null : c.getCommunityName();
    }
}
