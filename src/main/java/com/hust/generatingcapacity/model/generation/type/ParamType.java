package com.hust.generatingcapacity.model.generation.type;

import lombok.Getter;

@Getter
public enum ParamType {
    P(1), H(4), dH(3), Qp(5), Qo(2), C(6);

    // 获取优先级
    private final int priority;

    // 构造方法，初始化枚举项的优先级
    ParamType(int priority) {
        this.priority = priority;
    }

    // 比较优先级（可以用在排序等场景）
    public static int comparePriority(ParamType p1, ParamType p2) {
        return Integer.compare(p1.getPriority(), p2.getPriority());
    }
}
