package com.hust.generatingcapacity.model.generation.dispatch;

import org.ojalgo.optimisation.*;
import org.ojalgo.structure.Structure1D;
import java.math.BigDecimal;
import java.util.*;

public final class InfeasibleBoundCheck {

    /** 核心方法：分析无解模型的边界冲突 */
    public static void analyse(ExpressionsBasedModel model) {
        System.out.println("== 检查 Variable 上下界冲突 ==");
        for (Variable v : model.getVariables()) {
            BigDecimal lo = v.getLowerLimit();
            BigDecimal hi = v.getUpperLimit();
            if (lo != null && hi != null && lo.compareTo(hi) > 0) {
                System.out.printf("⚠️ %s 上下界矛盾: lower=%s > upper=%s%n",
                        v.getName(), lo, hi);
            }
        }

        System.out.println("\n== 检查 Expression 内部可能的区间冲突 ==");
        for (Expression e : model.getExpressions()) {
            if (!e.isConstraint()) continue;
            BigDecimal lo = e.getLowerLimit();
            BigDecimal hi = e.getUpperLimit();
            if (lo != null && hi != null && lo.compareTo(hi) > 0) {
                System.out.printf("⚠️ 约束 %s 本身上下界矛盾: lower=%s > upper=%s%n",
                        e.getName(), lo, hi);
            }
        }

        System.out.println("\n== 检查等式约束与变量边界的逻辑冲突 ==");
        for (Expression e : model.getExpressions()) {
            if (!e.isEqualityConstraint()) continue;
            // 对等式 a*x + ... = b，估算左右两端在当前变量上下界的取值范围
            BigDecimal b = e.getLowerLimit();
            BigDecimal minSum = BigDecimal.ZERO;
            BigDecimal maxSum = BigDecimal.ZERO;
            for (Structure1D.IntIndex key : e.getLinearKeySet()) {
                Variable v = model.getVariable(key);
                BigDecimal coef = e.get(key);
                BigDecimal lo = v.getLowerLimit() != null ? v.getLowerLimit() : BigDecimal.valueOf(-1e10);
                BigDecimal hi = v.getUpperLimit() != null ? v.getUpperLimit() : BigDecimal.valueOf(1e10);
                if (coef.signum() >= 0) {
                    minSum = minSum.add(coef.multiply(lo));
                    maxSum = maxSum.add(coef.multiply(hi));
                } else {
                    minSum = minSum.add(coef.multiply(hi));
                    maxSum = maxSum.add(coef.multiply(lo));
                }
            }
            if (b.compareTo(maxSum) > 0 || b.compareTo(minSum) < 0) {
                System.out.printf("⚠️ 等式约束 %s 无法在变量边界内实现: b=%s ∉ [%.3e, %.3e]%n",
                        e.getName(), b, minSum.doubleValue(), maxSum.doubleValue());
                for (Structure1D.IntIndex key : e.getLinearKeySet()) {
                    Variable v = model.getVariable(key);
                    System.out.printf("变量 %-10s coef=%8.3f  range=[%s, %s]%n",
                            v.getName(), e.get(key).doubleValue(), v.getLowerLimit(), v.getUpperLimit());
                }
            }
        }
    }
}

