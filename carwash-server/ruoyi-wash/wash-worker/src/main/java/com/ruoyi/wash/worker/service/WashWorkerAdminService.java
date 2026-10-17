package com.ruoyi.wash.worker.service;

import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.network.service.WashCommunityAdminService;
import com.ruoyi.wash.network.service.WashSiteQueryService;
import com.ruoyi.wash.worker.domain.WashWorker;
import com.ruoyi.wash.worker.domain.WorkerStatus;
import com.ruoyi.wash.worker.domain.WorkerWorkStatus;
import com.ruoyi.wash.worker.dto.AdminWorkerVO;
import com.ruoyi.wash.worker.mapper.WashWorkerMapper;
import com.ruoyi.wash.worker.security.WorkerTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 后台师傅（工作人员）管理。
 *
 * <p>为什么需要：师傅此前只能靠改 SQL 维护，运营无法自助增删、禁用离职师傅或重置密码——
 * 而这三件恰好是开业后每天都会发生的事。
 *
 * <p>安全口径：所有出参一律走 {@link AdminWorkerVO}（**不含 password 字段**），
 * 绝不把 WashWorker 实体直接返回，避免将来给实体加字段时被误带出去。
 */
@Service
public class WashWorkerAdminService {

    /** 工号 / 姓名的最小长度，与前端必填校验保持一致（前端校验不是防线，这里再守一次） */
    private static final int MIN_PASSWORD_LEN = 6;

    @Autowired
    private WashWorkerMapper workerMapper;

    /** 站点是 wash-network 域的数据，跨模块只调它的 Service，不碰它的 Mapper */
    @Autowired
    private WashSiteQueryService siteQuery;

    /** 服务小区同样是 wash-network 域的数据 */
    @Autowired
    private WashCommunityAdminService communityQuery;

    @Autowired
    private RedisCache redisCache;

    public List<AdminWorkerVO> list(String keyword, Long siteId, String status, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<AdminWorkerVO> rows = workerMapper.selectAdminList(keyword, siteId, status, offset, pageSize);
        Map<Long, String> siteNames = siteNames();
        Map<Long, String> communityNames = communityNames();
        rows.forEach(w -> decorate(w, siteNames, communityNames));
        return rows;
    }

    public long count(String keyword, Long siteId, String status) {
        return workerMapper.countAdmin(keyword, siteId, status);
    }

    public AdminWorkerVO get(Long workerId) {
        AdminWorkerVO vo = require(workerId);
        decorate(vo, siteNames(), communityNames());
        return vo;
    }

    public Long add(AdminWorkerVO worker) {
        checkBasic(worker);
        // 密码只在新增时给初始值，之后只能走 reset-password 改
        if (StringUtils.isEmpty(worker.getPassword())) {
            throw new ApiException(ErrorCode.A0001, "初始密码必填");
        }
        checkPassword(worker.getPassword());
        if (workerMapper.countByWorkerNo(worker.getWorkerNo()) > 0) {
            throw new ApiException(ErrorCode.D4005, "工号 " + worker.getWorkerNo() + " 已被占用");
        }
        if (worker.getStatus() == null) {
            worker.setStatus(WorkerStatus.NORMAL.name());
        }
        if (worker.getWorkStatus() == null) {
            worker.setWorkStatus(WorkerWorkStatus.OFFLINE.name());
        }
        workerMapper.insertWorker(worker);
        return worker.getWorkerId();
    }

    /**
     * 修改资料。**明确不接受 password**：改密码是另一个动作、另一个权限，
     * 混在一起容易出现「运营改個手机号，师傅被登出」的误操作。
     */
    public void edit(AdminWorkerVO worker) {
        AdminWorkerVO existed = require(worker.getWorkerId());
        checkBasic(worker);
        if (isChangingWorkerNo(existed, worker)) {
            throw new ApiException(ErrorCode.A0001, "工号不支持修改：如需变更请删除后新建（工号是登录凭证）");
        }
        workerMapper.updateWorker(worker);
        // 改成禁用 = 立刻把人踢下线，否则已登录的师傅还能继续操作到 token 自然过期
        if (WorkerStatus.DISABLED.name().equals(worker.getStatus())) {
            revokeTokens(worker.getWorkerId());
        }
    }

    public void remove(Long workerId) {
        require(workerId);
        workerMapper.logicDelete(workerId);
        revokeTokens(workerId);
    }

    /** 重置密码：运营最高频的操作之一（师傅忘密码）。重置后旧 token 立即失效。 */
    public void resetPassword(Long workerId, String password) {
        require(workerId);
        checkPassword(password);
        workerMapper.updatePassword(workerId, password);
        revokeTokens(workerId);
    }

    // ------------------------------------------------------------------ 内部

    private AdminWorkerVO require(Long workerId) {
        if (workerId == null) {
            throw new ApiException(ErrorCode.D4006);
        }
        AdminWorkerVO worker = workerMapper.selectAdminById(workerId);
        if (worker == null) {
            throw new ApiException(ErrorCode.D4006);
        }
        return worker;
    }

    private boolean isChangingWorkerNo(AdminWorkerVO existed, AdminWorkerVO incoming) {
        return incoming.getWorkerNo() != null
                && !incoming.getWorkerNo().equals(existed.getWorkerNo());
    }

    private void checkBasic(AdminWorkerVO worker) {
        if (StringUtils.isEmpty(worker.getWorkerNo())) {
            throw new ApiException(ErrorCode.A0001, "工号必填");
        }
        if (StringUtils.isEmpty(worker.getName())) {
            throw new ApiException(ErrorCode.A0001, "姓名必填");
        }
        if (worker.getStatus() != null && WorkerStatus.of(worker.getStatus()) == null) {
            throw new ApiException(ErrorCode.A0001, "未知师傅状态：" + worker.getStatus());
        }
        if (worker.getWorkStatus() != null && WorkerWorkStatus.of(worker.getWorkStatus()) == null) {
            throw new ApiException(ErrorCode.A0001, "未知上下班状态：" + worker.getWorkStatus());
        }
    }

    /**
     * 密码规则现在很松（只卡长度）：MVP 阶段密码是明文存储，
     * 强度规则应该和「改 BCrypt」一起做，分两次改容易顾此失彼。
     */
    private void checkPassword(String password) {
        if (StringUtils.isEmpty(password) || password.length() < MIN_PASSWORD_LEN) {
            throw new ApiException(ErrorCode.A0001, "密码至少 " + MIN_PASSWORD_LEN + " 位");
        }
    }

    /**
     * 吊销该师傅的全部登录态。
     *
     * <p>为什么是扫 key：token 是 UUID，Redis 里只有 token→workerId 的正向映射，
     * 没有 workerId→tokens 的反向索引。规模很小（同时在线的师傅数量是个位数），
     * 按前缀扫一次比改 TokenService 的签发逻辑更划算，也没有改动公共组件。
     */
    private void revokeTokens(Long workerId) {
        Collection<String> keys = redisCache.keys(WorkerTokenService.TOKEN_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return;
        }
        for (String key : keys) {
            Object owner = redisCache.getCacheObject(key);
            if (workerId.equals(owner)) {
                redisCache.deleteObject(key);
            }
        }
    }

    /** 站点 id → 名称，一次性查好避免逐行查库 */
    private Map<Long, String> siteNames() {
        return siteQuery.listSites().stream()
                .collect(Collectors.toMap(s -> s.getSiteId(), s -> s.getSiteName(), (a, b) -> a));
    }

    /** 补齐展示用字段，并确保 password 不带出去（VO 兼作入参，出参必须清干净） */
    private void decorate(AdminWorkerVO vo, Map<Long, String> siteNames, Map<Long, String> communityNames) {
        vo.setPassword(null);
        vo.setSiteName(vo.getSiteId() == null ? null : siteNames.get(vo.getSiteId()));
        vo.setCommunityName(vo.getCommunityId() == null ? null : communityNames.get(vo.getCommunityId()));
        // 中文状态名由后端枚举给出，前端只展示不翻译
        WorkerStatus status = WorkerStatus.of(vo.getStatus());
        vo.setStatusLabel(status == null ? vo.getStatus() : status.getLabel());
        // 上下班状态同样由后端枚举给中文名
        WorkerWorkStatus workStatus = WorkerWorkStatus.of(vo.getWorkStatus());
        vo.setWorkStatusLabel(workStatus == null ? vo.getWorkStatus() : workStatus.getLabel());
    }

    /** 小区 id → 名称，一次性查好避免逐行查库 */
    private Map<Long, String> communityNames() {
        return communityQuery.list(null, null, 1, 200).stream()
                .collect(Collectors.toMap(c -> c.getCommunityId(), c -> c.getCommunityName(), (a, b) -> a));
    }
}
