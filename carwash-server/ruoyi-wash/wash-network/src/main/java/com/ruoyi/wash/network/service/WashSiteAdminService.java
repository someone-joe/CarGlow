package com.ruoyi.wash.network.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.network.domain.WashSite;
import com.ruoyi.wash.network.mapper.WashSiteMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/** 后台站点（中央站）管理：增删改查。产能上限 / 营业时段 / 停业公告都配置在这里。 */
@Service
public class WashSiteAdminService {

    @Autowired
    private WashSiteMapper siteMapper;

    public List<WashSite> list(String keyword, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return siteMapper.selectList(keyword, offset, pageSize);
    }

    public int count(String keyword) {
        return siteMapper.count(keyword);
    }

    public WashSite get(Long siteId) {
        return siteMapper.selectById(siteId);
    }

    public Long add(WashSite site) {
        if (site.getSiteName() == null || site.getSiteName().isBlank()) {
            throw new ApiException(ErrorCode.A0001, "站点名称必填");
        }
        if (!siteMapper.selectList(site.getSiteName(), 0, 1).isEmpty()) {
            throw new ApiException(ErrorCode.A0001, "站点名称已存在");
        }
        siteMapper.insert(site);
        return site.getSiteId();
    }

    public void edit(WashSite site) {
        if (site.getSiteId() == null) {
            throw new ApiException(ErrorCode.A0001, "站点ID必填");
        }
        siteMapper.update(site);
    }

    public void remove(Long siteId) {
        siteMapper.logicDelete(siteId);
    }
}
