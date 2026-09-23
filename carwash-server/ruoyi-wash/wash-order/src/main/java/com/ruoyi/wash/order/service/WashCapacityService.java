package com.ruoyi.wash.order.service;

import com.ruoyi.wash.network.domain.WashSite;
import com.ruoyi.wash.network.service.WashSiteQueryService;
import com.ruoyi.wash.order.dto.CapacityVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(WashCapacityService.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final String CAPACITY_PREFIX = "capacity:";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private WashSiteQueryService siteQuery;

    @Value("${wash.default-site-id:1}")
    private Long defaultSiteId;

    /**
     * 初始化未来 N 天的产能（幂等）。
     *
     * <p>为什么不能用 SET 覆盖：任务会周期性执行，若直接 SET 会把已经扣掉的产能重置成上限，
     * 等于凭空放出名额导致超卖。因此一律用 SETNX —— key 已存在说明当天已有订单，保持原值。
     *
     * <p>daily_limit = 0（不限量）的站点不建 key：下单侧判定是「key 不存在 = 不限量」，
     * 建了反而会把它变成限量。
     */
    public void init(int daysAhead) {
        LocalDate today = LocalDate.now(ZONE);
        for (WashSite site : siteQuery.listSites()) {
            Integer limit = site.getDailyLimit();
            if (limit == null || limit <= 0) {
                continue;
            }
            for (int i = 0; i <= daysAhead; i++) {
                String day = today.plusDays(i).toString();
                String key = CAPACITY_PREFIX + site.getSiteId() + ":" + day;
                Boolean created = stringRedisTemplate.opsForValue().setIfAbsent(key, String.valueOf(limit));
                if (Boolean.TRUE.equals(created)) {
                    log.info("[产能初始化] siteId={} date={} limit={}", site.getSiteId(), day, limit);
                }
            }
        }
    }

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
        // 营业时段来自站点配置：改营业时间只改库，不改代码
        vo.setBusinessStart(site.getBusinessStart());
        vo.setBusinessEnd(site.getBusinessEnd());
        return vo;
    }
}
