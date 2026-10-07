package com.ruoyi.wash.worker.domain;

/**
 * 师傅状态枚举 —— 唯一定义处（SSOT），与 openapi.yaml 的 WorkerStatus 逐条一致。
 *
 * <p>此前 NORMAL / DISABLED 是 WashWorkerService 里的私有字符串常量，
 * 后台管理同样要判断状态，再写一份就会漂移，故提出为本枚举。
 *
 * <p>修改本枚举前，必须先修改 openapi.yaml 的 WorkerStatus。
 */
public enum WorkerStatus {

    NORMAL("正常"),
    DISABLED("已禁用");

    private final String label;

    WorkerStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 按枚举名解析；未知值返回 null，由调用方决定报错还是忽略 */
    public static WorkerStatus of(String name) {
        if (name == null) {
            return null;
        }
        for (WorkerStatus s : values()) {
            if (s.name().equals(name)) {
                return s;
            }
        }
        return null;
    }
}
