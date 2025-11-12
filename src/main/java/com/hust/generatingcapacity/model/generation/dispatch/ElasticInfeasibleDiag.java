package com.hust.generatingcapacity.model.generation.dispatch;

import org.ojalgo.optimisation.*;
import org.ojalgo.type.context.NumberContext;

import java.math.BigDecimal;
import java.util.*;

public final class ElasticInfeasibleDiag {

    /**
     * 用来承载“哪条约束、哪侧被放松、放松多少”
     */
    public static final class SlackInfo {
        public final String constraintName; // 约束名
        public final String side;           // "upper" / "lower" / "eq-upper" / "eq-lower"~
        public final double value;          // 松弛量（始终 ≥ 0）
        public final double lo;
        public final double hi;


        public SlackInfo(String constraintName, String side, double value, double lo, double hi) {
            this.constraintName = constraintName;
            this.side = side;
            this.value = value;
            this.lo = lo;
            this.hi = hi;
        }
    }

    /**
     * 主入口：在主模型 INFEASIBLE 时调用
     */
    public static SlackInfo diagnose(ExpressionsBasedModel originalModel) {

        // 复制一份模型，避免污染原模型
        ExpressionsBasedModel m = originalModel.copy();

        // 1. 清掉原目标，让诊断只关心“最小松弛”
        for (Expression e : m.getExpressions()) {
            if (e.isObjective()) {
                e.weight(BigDecimal.ZERO);
            }
        }

        // 2. 新建一个“松弛总和”的目标：min Σ slack
        Expression elasticObj = m.addExpression("__elastic_obj__").weight(1.0);

        // 防止遍历时结构被修改，先拷一份列表
        List<Expression> exprList = new ArrayList<>(m.getExpressions());

        for (Expression e : exprList) {

            if (!e.isConstraint()) continue;

            final String name = e.getName();

            // ====== 1) 剔除包含 "lambda" 或 "seg" 的约束 ======
            String lowerName = name.toLowerCase();
            if (lowerName.contains("lambda") || lowerName.contains("lam_") || lowerName.contains("seg")) {
                continue;
            }

            BigDecimal lo = e.getLowerLimit();
            BigDecimal hi = e.getUpperLimit();

            boolean hasLo = (lo != null);
            boolean hasHi = (hi != null);
            boolean isEq = e.isEqualityConstraint();

            if (isEq) {
                // ====== 2) 等式: a(x) = b  -> a(x) - s_eq_up + s_eq_lo = b ======
                Variable sEqUp = m.addVariable(name + "__s_eq_up").lower(0.0);
                Variable sEqLo = m.addVariable(name + "__s_eq_lo").lower(0.0);

                // a(x) - s_eq_up + s_eq_lo = b
                e.set(sEqUp, -1.0);  // 允许 a(x) > b   (上侧违反)
                e.set(sEqLo, +1.0);  // 允许 a(x) < b   (下侧违反)

                // 目标里对两个方向都计入惩罚
                elasticObj.set(sEqUp, 1.0);
                elasticObj.set(sEqLo, 1.0);

            } else {

                // ====== 3) 上界约束: a(x) ≤ hi -> a(x) - s_up ≤ hi ======
                if (hasHi) {
                    Variable sUp = m.addVariable(name + "__s_up").lower(0.0);
                    e.set(sUp, -1.0);        // a(x) - s_up ≤ hi → a(x) 可超过 hi，超量为 s_up
                    elasticObj.set(sUp, 1.0);
                }

                // ====== 4) 下界约束: a(x) ≥ lo -> a(x) + s_lo ≥ lo ======
                if (hasLo) {
                    Variable sLo = m.addVariable(name + "__s_lo").lower(0.0);
                    e.set(sLo, +1.0);        // a(x) + s_lo ≥ lo → a(x) 可低于 lo，低量为 s_lo
                    elasticObj.set(sLo, 1.0);
                }
            }
        }

        // 数值设置细一点，避免诊断被格式化破坏
        m.options.solution = NumberContext.of(16, 12);
        m.options.feasibility = NumberContext.of(8, 6);
        m.options.print = NumberContext.of(8, 4);
        m.options.validate = false;   // 我们自己判断

        Optimisation.Result r = m.minimise();

//        System.out.println("== Elastic 诊断模型求解状态: " + r.getState() + " ==");
        if (!r.getState().isFeasible()) {
            System.out.println("连“带松弛”的模型都不可行，说明模型结构本身就很不对劲。");
        }

        // ===== 读取 slack 变量的值，记录方向信息 =====
        double tol = 1e-7;
        List<SlackInfo> allSlacks = new ArrayList<>();

        for (Variable v : m.getVariables()) {
            String vname = v.getName();
            if (!vname.contains("__s_")) continue; // 只看我们加的松弛变量

            BigDecimal valBD = v.getValue();
            if (valBD == null) continue;
            double val = valBD.doubleValue();
            if (val <= tol) continue; // 太小当 0

            String baseName;
            String side;

            if (vname.endsWith("__s_up")) {
                // a(x) ≤ hi, 现在 a(x) 被允许 > hi
                baseName = vname.substring(0, vname.length() - "__s_up".length());
                side = "upper"; // 违反上界
            } else if (vname.endsWith("__s_lo")) {
                // a(x) ≥ lo, 现在 a(x) 被允许 < lo
                baseName = vname.substring(0, vname.length() - "__s_lo".length());
                side = "lower"; // 违反下界
            } else if (vname.endsWith("__s_eq_up")) {
                baseName = vname.substring(0, vname.length() - "__s_eq_up".length());
                side = "eq-upper"; // 等式上侧：a(x) > b
            } else if (vname.endsWith("__s_eq_lo")) {
                baseName = vname.substring(0, vname.length() - "__s_eq_lo".length());
                side = "eq-lower"; // 等式下侧：a(x) < b
            } else {
                continue;
            }

            //寻找原来左侧参数变化区间
            Expression expr = originalModel.getExpression(baseName);
            if (expr == null) {
                // 理论上不该发生，防御一下
                continue;
            }
            double[] lhsRange = estimateLhsInterval(originalModel, expr);
            double lhsLo = lhsRange[0];
            double lhsHi = lhsRange[1];

            allSlacks.add(new SlackInfo(baseName, side, val, lhsLo, lhsHi));
        }

        // 按松弛量从大到小排序
        allSlacks.sort((a, b) -> Double.compare(b.value, a.value));

//        System.out.println("== 诊断结果：需要松弛的约束（按松弛量降序）==");
        if (allSlacks.isEmpty()) {
            throw new RuntimeException("没有约束需要松弛（可能是纯数值问题或被筛掉的约束在作怪）。");
        } else {
            for (SlackInfo s : allSlacks) {
                String dirText = switch (s.side) {
                    case "upper" -> "违反上界（左边 > 上界，需放宽上界）";
                    case "lower" -> "违反下界（左边 < 下界，需放宽下界）";
                    case "eq-upper" -> "等式上侧违反（a(x) > b，可放宽等式右侧上限）";
                    case "eq-lower" -> "等式下侧违反（a(x) < b，可放宽等式右侧下限）";
                    default -> s.side;
                };
//                System.out.printf(
//                        "约束 %-25s %-24s  松弛量 ≈ %.6f 取值区间[%.3e, %.3e]%n",
//                        s.constraintName, dirText, s.value, s.lo, s.hi
//                );
            }
            return allSlacks.get(0); // 返回松弛量最大的那个
        }
    }

    private static double[] estimateLhsInterval(ExpressionsBasedModel m, Expression expr) {
        // 这里用一个比较大的“伪无穷”
        final double BIG = 1e300;

        double minSum = 0.0;
        double maxSum = 0.0;
        boolean unboundedBelow = false;
        boolean unboundedAbove = false;

        for (org.ojalgo.structure.Structure1D.IntIndex key : expr.getLinearKeySet()) {
            Variable v = m.getVariable(key);
            BigDecimal coefBD = expr.get(key);
            if (coefBD == null) continue;
            double c = coefBD.doubleValue();

            BigDecimal loBD = v.getLowerLimit();
            BigDecimal hiBD = v.getUpperLimit();
            Double lo = (loBD != null) ? loBD.doubleValue() : null;
            Double hi = (hiBD != null) ? hiBD.doubleValue() : null;

            if (lo == null && hi == null) {
                // 这个变量完全无界，表达式整体就无界
                unboundedBelow = true;
                unboundedAbove = true;
                break;
            }

            if (c >= 0) {
                if (lo == null) {
                    unboundedBelow = true;
                } else {
                    minSum += c * lo;
                }
                if (hi == null) {
                    unboundedAbove = true;
                } else {
                    maxSum += c * hi;
                }
            } else { // c < 0
                if (hi == null) {
                    unboundedBelow = true;
                } else {
                    minSum += c * hi;
                }
                if (lo == null) {
                    unboundedAbove = true;
                } else {
                    maxSum += c * lo;
                }
            }
        }

        double lhsLo = unboundedBelow ? -BIG : minSum;
        double lhsHi = unboundedAbove ? +BIG : maxSum;

        return new double[] { lhsLo, lhsHi };
    }

}


