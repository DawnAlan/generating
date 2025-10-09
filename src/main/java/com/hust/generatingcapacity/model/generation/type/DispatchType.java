package com.hust.generatingcapacity.model.generation.type;

import lombok.Getter;

@Getter
public enum DispatchType {
    MANUAL("人工调度"),
    AUTO("自动调度"),
    RULE_BASED("规则调度");

    private final String desc;

    DispatchType(String desc) {
        this.desc = desc;
    }

    public static DispatchType fromCode(String code) {
        return switch (code.toLowerCase()) {
            case "人工调度" -> MANUAL;
            case "自动调度" -> AUTO;
            case "规则调度" -> RULE_BASED;
            default -> throw new IllegalArgumentException("未知调度方式: " + code);
        };
    }

}

