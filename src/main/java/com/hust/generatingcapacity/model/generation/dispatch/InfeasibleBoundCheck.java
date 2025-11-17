package com.hust.generatingcapacity.model.generation.dispatch;

import org.ojalgo.optimisation.*;
import org.ojalgo.structure.Structure1D;
import java.math.BigDecimal;
import java.util.*;

public final class InfeasibleBoundCheck {

    /** 核心方法：分析无解模型的边界冲突 */
    public static void analyse(ExpressionsBasedModel model) {
        System.out.println("== 检查 NaN/Infinity（变量上下界 & 目标/约束）==");
        for (Variable v : model.getVariables()) {
            BigDecimal lo = v.getLowerLimit();
            BigDecimal hi = v.getUpperLimit();
            if (lo != null && (!lo.abs().toString().matches(".*\\d.*") || lo.toString().contains("NaN"))) {
                System.out.println("❌ Var NaN lower: " + v.getName() + " lo=" + lo);
            }
            if (hi != null && (!hi.abs().toString().matches(".*\\d.*") || hi.toString().contains("NaN"))) {
                System.out.println("❌ Var NaN upper: " + v.getName() + " hi=" + hi);
            }
        }

        for (Expression e : model.getExpressions()) {
            if (e.getLowerLimit() != null && (Double.isNaN(e.getLowerLimit().doubleValue()) || Double.isInfinite(e.getLowerLimit().doubleValue())))
                System.out.println("❌ Expr NaN/Inf lower: " + e.getName());
            if (e.getUpperLimit() != null && (Double.isNaN(e.getUpperLimit().doubleValue()) || Double.isInfinite(e.getUpperLimit().doubleValue())))
                System.out.println("❌ Expr NaN/Inf upper: " + e.getName());

            // 也扫扫系数
            for (var kv : e.getLinearEntrySet()) {
                BigDecimal coef = e.get(kv.getKey());
                if (coef == null || Double.isNaN(coef.doubleValue()) || Double.isInfinite(coef.doubleValue())) {
                    Variable vv = model.getVariable(kv.getKey());
                    System.out.println("❌ Expr coef NaN/Inf: " + e.getName() + " * " + (vv!=null?vv.getName():kv.getKey()));
                }
            }
        }
        System.out.println("\n== 检查不等式约束的可达性（区间松弛估计）==");
        for (Expression e : model.getExpressions()) {
            if (!e.isConstraint()) continue;

            BigDecimal minSum = BigDecimal.ZERO, maxSum = BigDecimal.ZERO;
            for (var kv : e.getLinearEntrySet()) {
                Variable v = model.getVariable(kv.getKey());
                if (v == null) continue;
                BigDecimal coef = e.get(kv.getKey());
                BigDecimal lo = v.getLowerLimit();
                BigDecimal hi = v.getUpperLimit();
                // 无界就跳过这项（而不是用 1e10 伪装）
                if (coef == null || lo == null || hi == null) {
                    minSum = null; maxSum = null; break;
                }
                if (coef.signum() >= 0) {
                    minSum = minSum.add(coef.multiply(lo));
                    maxSum = maxSum.add(coef.multiply(hi));
                } else {
                    minSum = minSum.add(coef.multiply(hi));
                    maxSum = maxSum.add(coef.multiply(lo));
                }
            }
            if (minSum == null || maxSum == null) {
                System.out.println("⚠️ " + e.getName() + " 含无界变量，跳过区间检测（若是 McCormick/λ 约束，这可能就是问题）");
                continue;
            }
            BigDecimal loB = e.getLowerLimit();
            BigDecimal hiB = e.getUpperLimit();
            if (hiB != null && minSum.compareTo(hiB) > 0) {
                System.out.printf("❌ 约束 %s 不可行：min(LHS)=%.6g > upper=%s%n", e.getName(), minSum.doubleValue(), hiB);
            }
            if (loB != null && maxSum.compareTo(loB) < 0) {
                System.out.printf("❌ 约束 %s 不可行：max(LHS)=%.6g < lower=%s%n", e.getName(), maxSum.doubleValue(), loB);
            }
        }

        System.out.println("\n== 检查 McCormick/分段绑定的有限界要求 ==");
        for (Expression e : model.getExpressions()) {
            String n = e.getName().toLowerCase();
            boolean suspectMc = n.startsWith("mc") || n.contains("mccormick");
            boolean suspectLam = n.contains("lambda") || n.contains("lam_") || n.contains("seg");

            if (suspectMc || suspectLam) {
                for (var kv : e.getLinearEntrySet()) {
                    Variable v = model.getVariable(kv.getKey());
                    if (v == null) continue;
                    BigDecimal lo = v.getLowerLimit(), hi = v.getUpperLimit();
                    if (lo == null || hi == null || !Double.isFinite(lo.doubleValue()) || !Double.isFinite(hi.doubleValue())) {
                        System.out.println("❌ "+ (suspectMc?"McCormick":"Piecewise") +" 相关变量无有限界: " + (v!=null?v.getName():kv.getKey()));
                    }
                }
            }
        }

        System.out.println("\n== 检查 Variable 上下界冲突 ==");
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

