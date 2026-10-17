package com.ruoyi.wash.worker.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 师傅周排班。表 wash_worker_schedule。
 *
 * <p>一周 7 天可配多段（如晚班 18:00-24:00 拆两段）；
 * 时间用「当日分钟数」（540 = 09:00）存储，跨天排班（22:00-02:00）由 endMinute 超过 1440 表示。
 */
public class WashWorkerSchedule extends BaseEntity {

    private Long id;
    private Long workerId;
    /** 星期：1=周一 ... 7=周日 */
    private Integer weekDay;
    /** 开始时间（当日分钟数，0-1439） */
    private Integer startMinute;
    /** 结束时间（当日分钟数，可 >1440 表示跨天） */
    private Integer endMinute;
    private String enabled;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWorkerId() {
        return workerId;
    }

    public void setWorkerId(Long workerId) {
        this.workerId = workerId;
    }

    public Integer getWeekDay() {
        return weekDay;
    }

    public void setWeekDay(Integer weekDay) {
        this.weekDay = weekDay;
    }

    public Integer getStartMinute() {
        return startMinute;
    }

    public void setStartMinute(Integer startMinute) {
        this.startMinute = startMinute;
    }

    public Integer getEndMinute() {
        return endMinute;
    }

    public void setEndMinute(Integer endMinute) {
        this.endMinute = endMinute;
    }

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }
}
