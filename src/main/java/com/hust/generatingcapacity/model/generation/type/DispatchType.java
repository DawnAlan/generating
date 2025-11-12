package com.hust.generatingcapacity.model.generation.type;

import lombok.Getter;

@Getter
public enum DispatchType {

    RULE_OPTIMIZE("规程优化"),
    RULE_BASED("规则调度"),
    PRE_CONDITION("预设条件");

    private final String desc;

    DispatchType(String desc) {
        this.desc = desc;
    }

    public static DispatchType fromCode(String code) {
        return switch (code.toLowerCase()) {
            case "预设条件" -> PRE_CONDITION;
            case "规程优化" -> RULE_OPTIMIZE;
            case "规则调度" -> RULE_BASED;
            default -> throw new IllegalArgumentException("未知调度方式: " + code);
        };
    }

}

