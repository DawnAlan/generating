package com.hust.generatingcapacity.model.generation.type;

import java.util.Arrays;

public enum ParamBoundType { // 纯“上下界”语义

    Qo_MIN("Qo", true),
    Qo_MAX("Qo", false),
    Qp_MIN("Qp", true),
    Qp_MAX("Qp", false),
    H_MIN("H", true),
    H_MAX("H", false),
    dH_MIN("dH", true),
    dH_MAX("dH", false),
    C_MIN("C", true),
    C_MAX("C", false),
    P_MIN("P", true),
    P_MAX("P", false);

    private final String variable;
    private final boolean isMin;

    ParamBoundType(String variable, boolean isMin) {
        this.variable = variable;
        this.isMin = isMin;
    }

    public String variable() { return variable; }

    // ---- 工具方法 ----
    public static ParamBoundType getMin(String variable) {
        return Arrays.stream(values())
                .filter(e -> e.variable.equals(variable) && e.isMin)
                .findFirst()
                .orElse(null);
    }

    public static ParamBoundType getMax(String variable) {
        return Arrays.stream(values())
                .filter(e -> e.variable.equals(variable) && !e.isMin)
                .findFirst()
                .orElse(null);
    }
}

