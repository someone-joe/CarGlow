package com.ruoyi.wash.selfwash.domain;

/**
 * 车型枚举 —— 自助洗车域唯一定义处（SSOT），与 openapi.yaml 的 vehicleType 逐条一致。
 *
 * <p>为什么要收车型：不同车型占用的工位空间、水量、耗时都有差异，运营调度与工位安排要用；
 * 顺带解决"自助下单没有车牌就无法现场核身"的问题（车牌随之改为必填）。
 *
 * <p>OTHER 是兜底：用户的车不在常见分类里时也能下单，不因为分类不全把客户挡在门外。
 * 真出现高频的 OTHER，再考虑加枚举值——**不要在前端私自扩展名单**。
 */
public enum VehicleType {

    SEDAN("小轿车"),
    SUV("SUV"),
    MPV("商务车"),
    OTHER("其他");

    private final String label;

    VehicleType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 按枚举名解析；未知值返回 null，由调用方决定报错 */
    public static VehicleType of(String name) {
        if (name == null) {
            return null;
        }
        for (VehicleType t : values()) {
            if (t.name().equals(name)) {
                return t;
            }
        }
        return null;
    }

    /** 取展示名：未知值原样返回，别把脏数据显示成空白 */
    public static String labelOf(String name) {
        VehicleType t = of(name);
        return t == null ? name : t.getLabel();
    }
}
