package com.hust.generatingcapacity.model.generation.dispatch;

import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PiecewiseLambdas {
    public final List<Variable> lambdas;   // λ[i]
    final List<Variable> segments;  // z[s]
    final double[] Hgrid;           // 水位网格 H[i]

    public PiecewiseLambdas(List<Variable> lambdas,
                            List<Variable> segments,
                            double[] hgrid) {
        this.lambdas = lambdas;
        this.segments = segments;
        this.Hgrid = hgrid;
    }

    public int size() {
        return lambdas.size();
    }

    // 构建 Ha 的分段线性化骨架（带 SOS2 结构）
    public static PiecewiseLambdas addPiecewiseHa(ExpressionsBasedModel model,
                                                  Variable Ha,
                                                  double[] Hgrid) {
        if (Hgrid == null || Hgrid.length < 2) {
            throw new IllegalArgumentException("Hgrid 至少需要两个点。");
        }
        // 1. 检查 Hgrid 严格递增
        for (int i = 1; i < Hgrid.length; i++) {
            if (!(Hgrid[i] > Hgrid[i - 1])) {
                throw new IllegalArgumentException("Hgrid 必须严格递增: " + Arrays.toString(Hgrid));
            }
        }

        final int n = Hgrid.length;
        final int sCount = n - 1; // 段数 = 点数 - 1

        // 2. λ[i] ∈ [0,1]
        List<Variable> lambdas = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Variable lam = model.addVariable("lam_" + i);
            lam.lower(0.0).upper(1.0);
            lambdas.add(lam);
        }

        // 3. 段选择变量 z[s] ∈ {0,1}
        List<Variable> zList = new ArrayList<>(sCount);
        for (int s = 0; s < sCount; s++) {
            Variable z = model.addVariable("seg_" + s);
            z.binary();
            zList.add(z);
        }

        // 4. Σ λ = 1
        Expression sumLam = model.addExpression("sum_lambda");
        for (Variable lam : lambdas) {
            sumLam.set(lam, 1.0);
        }
        sumLam.level(1.0);

        // 5. Ha = Σ H[i] * λ[i]
        Expression defHa = model.addExpression("Ha_by_lambda");
        defHa.set(Ha, -1.0);
        for (int i = 0; i < n; i++) {
            defHa.set(lambdas.get(i), Hgrid[i]);
        }
        defHa.level(0.0);

        // 6. Σ z = 1 （只选一个 segment）
        Expression sumZ = model.addExpression("sum_seg");
        for (Variable z : zList) {
            sumZ.set(z, 1.0);
        }
        sumZ.level(1.0);

        // 7. 相邻性约束：λ 只允许落在选中的相邻两点上
        // λ[0] ≤ z[0]
        model.addExpression("lam_0_link")
                .set(lambdas.get(0), 1.0)
                .set(zList.get(0), -1.0)
                .upper(0.0);

        // λ[i] ≤ z[i-1] + z[i]  (i = 1..n-2)
        for (int i = 1; i <= n - 2; i++) {
            Expression link = model.addExpression("lam_" + i + "_link");
            link.set(lambdas.get(i), 1.0);
            link.set(zList.get(i - 1), -1.0);
            link.set(zList.get(i), -1.0);
            link.upper(0.0);
        }

        // λ[n-1] ≤ z[n-2]
        model.addExpression("lam_last_link")
                .set(lambdas.get(n - 1), 1.0)
                .set(zList.get(n - 2), -1.0)
                .upper(0.0);

        return new PiecewiseLambdas(lambdas, zList, Hgrid);
    }

    /**
     * 把任意“跟随 Ha 的量”用同一套 λ 绑定：
     * target = Σ coef[i] * λ[i]
     * 其中 coef[i] 是该量在 Hgrid[i] 处的函数值（与 addPiecewiseHa 用的 Hgrid 同一网格）。
     * 例：Va = Σ V[i]*λ[i]（V 为绝对库容，单位统一！）
     *     Lc = Σ Lc[i]*λ[i]（Lc 为耗水率值）
     */
    public static void linkLinearByLambda(ExpressionsBasedModel model,
                                          Variable target,
                                          PiecewiseLambdas lam,
                                          double[] coefOnSameHGrid) {
        if (coefOnSameHGrid == null || coefOnSameHGrid.length != lam.size()) {
            throw new IllegalArgumentException("coef 长度必须与 λ 数量一致，且与 Hgrid 一一对应。");
        }
        Expression expr = model.addExpression(target.getName() + "_by_lambda");
        expr.set(target, -1.0);
        for (int i = 0; i < lam.size(); i++) {
            expr.set(lam.lambdas.get(i), coefOnSameHGrid[i]);
        }
        expr.level(0.0);
    }


}
