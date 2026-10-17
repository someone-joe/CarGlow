package com.ruoyi.wash.worker.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.worker.domain.WashWorkerSchedule;
import com.ruoyi.wash.worker.mapper.WashWorkerScheduleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 师傅周排班。
 *
 * <p>「没配排班」与「配了但今天不在班」是两回事：一条都没有时不限制派单
 * （否则忘记配排班的师傅永远接不到单，且没有任何提示）；配了才按表判断。
 */
@Service
public class WashWorkerScheduleService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired
    private WashWorkerScheduleMapper scheduleMapper;

    public List<WashWorkerSchedule> listByWorker(Long workerId) {
        return scheduleMapper.selectByWorkerId(workerId);
    }

    /** 整体覆盖周排班：一天可多段，段内时间必须合法 */
    @Transactional(rollbackFor = Exception.class)
    public void save(Long workerId, List<WashWorkerSchedule> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            // 允许清空：清空 = 不限制派单时段
            scheduleMapper.deleteByWorkerId(workerId);
            return;
        }
        for (WashWorkerSchedule s : schedules) {
            if (s.getWeekDay() == null || s.getWeekDay() < 1 || s.getWeekDay() > 7) {
                throw new ApiException(ErrorCode.A0001, "星期必须是 1-7（1=周一）");
            }
            if (s.getStartMinute() == null || s.getEndMinute() == null) {
                throw new ApiException(ErrorCode.A0001, "排班起止时间不能为空");
            }
            if (s.getStartMinute() < 0 || s.getStartMinute() > 1439) {
                throw new ApiException(ErrorCode.A0001, "开始时间必须在 00:00-23:59 之间");
            }
            // 允许跨天（如 22:00-02:00 记为 1320-1560），最长不超过 24 小时
            if (s.getEndMinute() <= s.getStartMinute() || s.getEndMinute() - s.getStartMinute() > 1440) {
                throw new ApiException(ErrorCode.A0001, "结束时间必须晚于开始时间，且不超过 24 小时");
            }
            if (s.getEnabled() == null) {
                s.setEnabled("Y");
            }
            s.setWorkerId(workerId);
        }
        // 先删后插：整表覆盖，避免「改了时段但旧段还在」的漂移
        scheduleMapper.deleteByWorkerId(workerId);
        for (WashWorkerSchedule s : schedules) {
            scheduleMapper.insert(s);
        }
    }

    /** 今天是否在班（派单准入） */
    public boolean isOnDutyToday(Long workerId) {
        List<WashWorkerSchedule> rows = scheduleMapper.selectByWorkerId(workerId);
        if (rows.isEmpty()) {
            return true;
        }
        int today = LocalDate.now(ZONE).getDayOfWeek().getValue();
        int now = LocalTime.now(ZONE).getHour() * 60 + LocalTime.now(ZONE).getMinute();
        for (WashWorkerSchedule s : rows) {
            if (!"Y".equals(s.getEnabled()) || s.getWeekDay() == null) {
                continue;
            }
            if (s.getWeekDay() != today) {
                continue;
            }
            int start = s.getStartMinute() == null ? 0 : s.getStartMinute();
            int end = s.getEndMinute() == null ? 0 : s.getEndMinute();
            // 跨天时段：当前时刻 + 24h 仍落在区间内，说明还在前一天的班里
            if ((now >= start && now < end) || (end > 1440 && now + 1440 < end)) {
                return true;
            }
        }
        return false;
    }
}
