package com.ruoyi.wash.worker.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.worker.domain.WashWorker;
import com.ruoyi.wash.worker.dto.WorkerLoginVO;
import com.ruoyi.wash.worker.mapper.WashWorkerMapper;
import com.ruoyi.wash.worker.security.WorkerTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 师傅域业务：工号登录。
 *
 * <p>密码校验说明：MVP 阶段 wash_worker.password 存明文、此处直接比对，
 * 仅为本地联调方便。上线前改为 BCrypt 校验时只动本方法，其余零改动。
 */
@Service
public class WashWorkerService {

    private static final String STATUS_NORMAL = "NORMAL";

    @Autowired
    private WashWorkerMapper workerMapper;

    @Autowired
    private WorkerTokenService tokenService;

    public WorkerLoginVO login(String workerNo, String password) {
        WashWorker worker = workerMapper.selectByWorkerNo(workerNo);
        if (worker == null) {
            throw new ApiException(ErrorCode.D4001, "工号不存在");
        }
        if (!matches(worker.getPassword(), password)) {
            throw new ApiException(ErrorCode.D4002, "密码或验证码错误");
        }
        if (!STATUS_NORMAL.equals(worker.getStatus())) {
            throw new ApiException(ErrorCode.D4003, "师傅账号已被禁用");
        }
        if (worker.getSiteId() == null) {
            throw new ApiException(ErrorCode.D4004, "师傅未绑定站点");
        }

        WorkerLoginVO vo = new WorkerLoginVO();
        vo.setToken(tokenService.createToken(worker.getWorkerId()));
        vo.setExpireAt(tokenService.getExpireAt());
        vo.setWorkerId(worker.getWorkerId());
        vo.setWorkerNo(worker.getWorkerNo());
        vo.setName(worker.getName());
        vo.setSiteId(worker.getSiteId());
        vo.setStatus(worker.getStatus());
        return vo;
    }

    /**
     * 取当前师傅所属站点。任务池必须按站点过滤（师傅只能看本站点单），
     * 未绑定站点的师傅直接拒绝，避免返回全量订单。
     */
    public Long requireSiteId(Long workerId) {
        WashWorker worker = workerMapper.selectByWorkerId(workerId);
        if (worker == null) {
            throw new ApiException(ErrorCode.D4001);
        }
        if (worker.getSiteId() == null) {
            throw new ApiException(ErrorCode.D4004);
        }
        return worker.getSiteId();
    }

    /** 明文比对（MVP）。改 BCrypt 时替换本方法即可。 */
    private boolean matches(String stored, String input) {
        return stored != null && stored.equals(input);
    }
}
