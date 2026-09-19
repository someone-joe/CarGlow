package com.ruoyi.wash.order.service;

import com.ruoyi.wash.network.domain.WashSite;
import com.ruoyi.wash.network.service.WashSiteQueryService;
import com.ruoyi.wash.order.dto.CapacityVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 产能查询。契约：openapi.yaml GET /api/v1/capacity。
 *
 * <p>产能按「站点 + 日期」计数，key 规范见技术方案 5.3，与下单扣减、取消回补是同一个 key。
 * 最晚存钥匙时间与承诺还车时间取自站点配置，不在代码里写死（红线 5）。
 *
 * <p>注意：key 不存在表示当日尚未初始化产能（下单侧按"不限量"处理），
 * 此时剩余量按站点上限返回；等"每日产能初始化"任务开工后这里会始终读到真实值。
 */
@Service
public class WashCapacityService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final String CAPACITY_PREFIX = "capacity:";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private WashSiteQueryService siteQuery;

    @Value("${wash.default-site-id:1}")
    private Long defaultSiteId;

    public CapacityVO capacity(String date) {
        String day = (date == null || date.isBlank()) ? LocalDate.now(ZONE).toString() : date;
        WashSite site = siteQuery.require(defaultSiteId);

        int total = site.getDailyLimit() == null ? 0 : site.getDailyLimit();
        String value = stringRedisTemplate.opsForValue().get(CAPACITY_PREFIX + site.getSiteId() + ":" + day);
        int remaining = value == null ? total : Integer.parseInt(value);

        CapacityVO vo = new CapacityVO();
        vo.setDate(day);
        vo.setTotal(total);
        vo.setRemaining(remaining);
        vo.setUsed(Math.max(0, total - remaining));
        vo.setSoldOut(total > 0 && remaining <= 0);
        vo.setDepositDeadline(site.getDepositDeadline());
        vo.setPromiseReturnTime(site.getPromiseReturnTime());
        return vo;
    }
}
