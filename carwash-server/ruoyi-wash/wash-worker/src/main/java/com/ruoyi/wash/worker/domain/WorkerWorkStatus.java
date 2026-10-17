package com.ruoyi.wash.worker.domain;

/**
 * 师傅上下班状态（T13）。与 wash_worker.status（NORMAL/DISABLED 账号是否可用）是两回事：
 * status 由后台控制账号生死，workStatus 由师傅自己打卡。
 *
 * <p>派单只看 status=NORMAL 且 workStatus=ONLINE；BUSY 表示在忙但仍在线（可按需派）。
 */
public enum WorkerWorkStatus {

    OFFLINE("下班"),
    ONLINE("上班"),
    BUSY("忙碌");

    private final String label;

    WorkerWorkStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 按枚举名解析；未知值返回 null，由调用方决定报错还是忽略 */
    public static WorkerWorkStatus of(String name) {
        if (name == null) {
            return null;
        }
        for (WorkerWorkStatus s : values()) {
            if (s.name().equals(name)) {
                return s;
            }
        }
        return null;
    }
}
