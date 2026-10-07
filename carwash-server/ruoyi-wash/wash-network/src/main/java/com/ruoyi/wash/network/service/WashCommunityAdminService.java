package com.ruoyi.wash.network.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.network.domain.WashCommunity;
import com.ruoyi.wash.network.mapper.WashCommunityMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/** 后台小区管理：增删改查。accessAuth = N 时禁止该小区下单（地库未授权）。 */
@Service
public class WashCommunityAdminService {

    @Autowired
    private WashCommunityMapper communityMapper;

    public List<WashCommunity> list(String keyword, Long siteId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return communityMapper.selectList(keyword, siteId, offset, pageSize);
    }

    public int count(String keyword, Long siteId) {
        return communityMapper.count(keyword, siteId);
    }

    public WashCommunity get(Long communityId) {
        return communityMapper.selectById(communityId);
    }

    public Long add(WashCommunity community) {
        if (community.getCommunityName() == null || community.getCommunityName().isBlank()
                || community.getSiteId() == null) {
            throw new ApiException(ErrorCode.A0001, "小区名称与所属站点必填");
        }
        if (!communityMapper.selectList(community.getCommunityName(), community.getSiteId(), 0, 1).isEmpty()) {
            throw new ApiException(ErrorCode.A0001, "该站点下小区名称已存在");
        }
        communityMapper.insert(community);
        return community.getCommunityId();
    }

    public void edit(WashCommunity community) {
        if (community.getCommunityId() == null) {
            throw new ApiException(ErrorCode.A0001, "小区ID必填");
        }
        communityMapper.update(community);
    }

    public void remove(Long communityId) {
        communityMapper.logicDelete(communityId);
    }
}
