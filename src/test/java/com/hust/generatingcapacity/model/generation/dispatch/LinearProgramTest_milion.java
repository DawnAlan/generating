package com.hust.generatingcapacity.model.generation.dispatch;

import com.hust.generatingcapacity.GeneratingCapacityApplication;
import com.hust.generatingcapacity.dto.StationInfDTO;
import com.hust.generatingcapacity.iservice.IHydropowerStationService;
import com.hust.generatingcapacity.model.generation.domain.CodeValue;
import com.hust.generatingcapacity.model.generation.domain.ConstraintData;
import com.hust.generatingcapacity.model.generation.domain.StationData;
import com.hust.generatingcapacity.model.generation.type.ParamType;
import com.hust.generatingcapacity.model.generation.vo.BoundPair;
import org.junit.jupiter.api.Test;
import org.ojalgo.optimisation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@SpringBootTest(classes = GeneratingCapacityApplication.class)
public class LinearProgramTest_milion {
    @Autowired
    private IHydropowerStationService hydropowerStationService;

    //    @Test
//    void testOjAIgo() {
//        System.setProperty("org.ojalgo.announcements", "false");
//        ExpressionsBasedModel model = new ExpressionsBasedModel();
//        Variable p1 = model.addVariable("p1").lower(0); // 连续
//        Variable p2 = model.addVariable("p2").lower(0);
//        Variable u1 = model.addVariable("u1").binary();   // 0-1
//        Variable u2 = model.addVariable("u2").binary();
//
//        // 0 ≤ p_t ≤ 5*u_t
//        model.addExpression("p1_cap").set(p1, 1).set(u1, -5).upper(0); // p1 - 5*u1 ≤ 0
//        model.addExpression("p2_cap").set(p2, 1).set(u2, -5).upper(0);
//
//        // 总资源（用水）约束
//        model.addExpression("sum_cap").set(p1, 1).set(p2, 1).upper(6);
//
//        // 爬坡：p2 - p1 ≤ 3
//        model.addExpression("ramp").set(p2, 1).set(p1, -1).upper(3);
//
//        // 目标：max 50*p1 + 30*p2 - 10*(u1+u2)
//        model.addExpression("obj")
//                .set(p1, 50).set(p2, 30).set(u1, -10).set(u2, -10)
//                .weight(1.0);
//
//        Optimisation.Result rs = model.maximise();
//        System.out.println("State = " + rs.getState());
//        if (rs.getState().isFeasible()) {
//            System.out.printf("p1=%.3f, p2=%.3f, u1=%s, u2=%s%n",
//                    p1.getValue().doubleValue(), p2.getValue().doubleValue(),
//                    u1.getValue().intValue(), u2.getValue().intValue());
//            System.out.println("obj=" + rs.getValue());
//        } else {
//            System.out.println("不可行/未收敛");
//        }
//    }

    @Test
    void testSimplex() {
        // 入库径流
        double Qin = 825.0;
        // 时段初水位
        double Hb = 1834.09;
        double Tailb = 1701.36;
        // 时间间隔，单位秒
        int t = 86400;

        //获取参数边界
        StationInfDTO dto = hydropowerStationService.get("猴子岩");
        StationData stationData = hydropowerStationService.changeToStationData(dto);
        Map<String, Object> condition = new ConstraintEnvBuilder().conditionBuild(11, Hb, 1, t, Qin);
        List<ConstraintData> constraints = stationData.getConstraints();
        Map<ParamType, BoundPair> cound = stationData.setInitialBoundPair();
        for (ConstraintData constraint : constraints) {
            if (constraint.isConditionActive(constraint.getCondition(), condition)) {
                List<String> paramList = constraint.getParam();
                System.out.println(paramList);
                new ConstraintData().getParamBoundPair(paramList, condition, cound);
            }
        }
        System.out.println(cound.toString());

        //获取特征曲线
        List<CodeValue> reservoirStorageLine = stationData.getReservoirStorageLine();
        List<CodeValue> waterConsumptionLine = stationData.getWaterConsumptionLine();

        // 时段初库容
        double Vb = CodeValue.linearInterpolation(Hb, reservoirStorageLine);
        double Vp_min = cound.get(ParamType.Qp).getMinVal() * t / 1e6;
        double Vp_max = cound.get(ParamType.Qp).getMaxVal() * t / 1e6;
        double L_min = CodeValue.getMinValue(waterConsumptionLine);
        double L_max = CodeValue.getMaxValue(waterConsumptionLine);
        double genL = Math.max(cound.get(ParamType.P).getMinVal() * t / 3600, Vp_min / L_max * 1e3);
        double genU = Math.min(cound.get(ParamType.P).getMaxVal() * t / 3600, Vp_max / L_min * 1e3);
        System.setProperty("shut.up.ojAlgo", "true");
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // 发电流量
        Variable Qp = addVar(model, "Qp", cound.get(ParamType.Qp).getMinVal(), cound.get(ParamType.Qp).getMaxVal());
        // 发电水量 Vp
        Variable Vp = addVar(model, "Vp", Vp_min, Vp_max);
        // 出库流量
        Variable Qo = addVar(model, "Qo", cound.get(ParamType.Qo).getMinVal(), cound.get(ParamType.Qo).getMaxVal());
        // 时段末水位
        Variable Ha = addVar(model, "Ha", cound.get(ParamType.H).getMinVal(), cound.get(ParamType.H).getMaxVal());
        // 末水位对应绝对库容（决策变量）
        Variable Va = addVar(model, "Va", CodeValue.linearInterpolation(cound.get(ParamType.H).getMinVal(), reservoirStorageLine), CodeValue.linearInterpolation(cound.get(ParamType.H).getMaxVal(), reservoirStorageLine));
        // 水位变幅
        Variable dH = addVar(model, "dH", cound.get(ParamType.dH).getMinVal(), cound.get(ParamType.dH).getMaxVal());
        // 水量变幅 dV（水位库容曲线为百万）
        Variable dV = addVar(model, "dV", CodeValue.difference(Hb, cound.get(ParamType.H).getMinVal(), reservoirStorageLine), CodeValue.difference(Hb, cound.get(ParamType.H).getMaxVal(), reservoirStorageLine));
        // 发电负荷
        Variable Gen = addVar(model, "Gen", genL, genU);

        // ===== 添加约束 =====
        // 1. Qp ≤ Qo （发电流量不超过总出库流量）
        Expression constr1 = model.addExpression("Qp_le_Qo")
                .set(Qp, 1.0)    // 系数 * Qp
                .set(Qo, -1.0);  // 系数 * Qo  => 表达式 = Qp - Qo
        constr1.upper(0.0);       // 约束: Qp - Qo ≤ 0  【即 Qp ≤ Qo】
//        constr1.level(0.0);

        // 2. 水位变幅定义: dH = Ha - Hb
        Expression constr2 = model.addExpression("dH_def")
                .set(Ha, 1.0)
                .set(dH, -1.0);
        constr2.level(Hb);        // 约束: Ha - dH = Hb  【即 dH = Ha - Hb】

        // 3. 水量变幅定义: Vb = Va - dV
        Expression constr3 = model.addExpression("dV_def")
                .set(dV, -1.0)
                .set(Va, 1.0);
        constr3.level(Vb);

        // 4. 发电水量与流量关系: Vp = Qp * t
        Expression constr4 = model.addExpression("Vp_def")
                .set(Vp, 1e6)
                .set(Qp, -t);
        constr4.level(0.0);       // 约束: Vp - t*Qp = 0  【即 Vp = Qp * t】

        // 5. 出库水量与流量关系: dV = (Qin-Qo) * t
        Expression constr5 = model.addExpression("dV_def_Q")
                .set(dV, 1e6)
                .set(Qo, t);
        constr5.level(Qin * t);


        // 发电计算
        Variable Lc = null;
        double L = 0.0;
        if (waterConsumptionLine.size() > 1) {//有细致的耗水率曲线
            Lc = addVar(model, "Lc", CodeValue.getMinValue(waterConsumptionLine), CodeValue.getMaxValue(waterConsumptionLine));
            if (!stationData.getIsWaterConsumption()) {
                waterConsumptionLine.forEach(cv -> cv.setCode(cv.getCode() + Tailb));
            }
            CodeValue.sampleToGrid(reservoirStorageLine, waterConsumptionLine);// 重采样到同一网格
            double[] Hgrid = reservoirStorageLine.stream().mapToDouble(CodeValue::getCode).toArray();
            PiecewiseLambdas lam = addPiecewiseHa(model, Ha, Hgrid); // 构建 Ha 分段线性化骨架
            double[] Lgrid = waterConsumptionLine.stream().mapToDouble(CodeValue::getValue).toArray();
            linkLinearByLambda(model, Lc, lam, Lgrid); // 把 Lc 用同一套 λ 绑定
            double[] Vgrid = reservoirStorageLine.stream().mapToDouble(CodeValue::getValue).toArray();
            linkLinearByLambda(model, Va, lam, Vgrid); // 把 Va 用同一套 λ 绑定
            // McCormick：Vp = Gen * L耗
            addMcCormickBilinear(model, Vp, Gen, Lc, genL, genU, L_min, L_max);
        } else {
            L = waterConsumptionLine.get(0).getValue();
            double[] Hgrid = reservoirStorageLine.stream().mapToDouble(CodeValue::getCode).toArray();
            PiecewiseLambdas lam = addPiecewiseHa(model, Ha, Hgrid); // 构建 Ha 分段线性化骨架
            double[] Vgrid = reservoirStorageLine.stream().mapToDouble(CodeValue::getValue).toArray();
            linkLinearByLambda(model, Va, lam, Vgrid); // 把 Va 用同一套 λ 绑定
            // 6. 发电能力与耗水率/发电流量的关系
            Expression constr6 = model.addExpression("Gen_def")
                    .set(Vp, 1)
                    .set(Gen, -L / 1e3);
            constr6.level(0.0);
        }


        // === ★ 分段线性化：Va = f(Ha)，用 λ-凸组合 + 段选择 z[s] ===
//        addPiecewiseHV(model, Ha, Va, reservoirStorageLine); // 把 Va 与 Ha 按散点做线性插值（内嵌在 MILP 里）
        // === ★ 分段线性化：Lc = f(Ha)，用 λ-凸组合 + 段选择 z[s] ===
//        addPiecewiseHV(model, Ha, Lc, waterConsumptionLine);
        // === McCormick：Vp = Gen * L耗 ===
//        addMcCormickBilinear(model, Vp, Gen, Lc, genL, genU, L_min, L_max);

        // === 目标：最大化 Gen ===
        Gen.weight(-1.0);
        model.options.validate = false;      // 求解后自动检查所有约束是否被满足
        model.options.logger_detailed  = true;      // 让控制台输出迭代过程（调试阶段很有用）
        model.options.time_abort = 30000;    // 运行 30 秒后强制停止
        model.options.iterations_abort = 1000000; // 达到最大迭代数后停止
//        model.options.feasibility = NumberContext.of(12, 8); // 控制判断“等于”的容差
        Optimisation.Result rs = model.maximise();

        // ====== 打印结果 ======
        System.out.println("===== 计算结果 =====");
        System.out.println("State = " + rs.getState());
        System.out.printf("Gen=%.3f, Qp=%.3f, Qo=%.3f%n",
                Gen.getValue().doubleValue(), Qp.getValue().doubleValue(), Qo.getValue().doubleValue());
        System.out.printf("Ha=%.3f,Hb=%.3f, dH=%.3f, Va=%.0f,Vb=%.0f, dV=%.0f%n",
                Ha.getValue().doubleValue(), Hb, dH.getValue().doubleValue(),
                Va.getValue().doubleValue(), Vb, dV.getValue().doubleValue());
        System.out.printf("Vp=%.0f, L耗=%.4f%n",
                Vp.getValue().doubleValue(), Lc == null ? L : Lc.getValue().doubleValue());


    }

    /**
     * 在模型里加入：Va = f(Ha) 的“分段线性化”
     * 形式：λ-凸组合 + 相邻段选择 z[s]，确保 λ 只在一个 segment 的两端点非零
     * 要求：H 递增；Va 与 Ha 都是“决策变量”，dV 外面用 dV = V(Hb) - Va 约束连接。
     */
    private static void addPiecewiseHV(ExpressionsBasedModel model,
                                       Variable Ha, Variable Va,
                                       List<CodeValue> reservoirStorageLine) {
        int n = reservoirStorageLine.size();          // breakpoints: i = 0..n-1
        int sCount = n - 1;        // segments:    s = 0..n-2

        // λ[i] 连续变量，0 ≤ λ ≤ 1
        List<Variable> lambdas = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Variable lam = model.addVariable("lam_" + i).lower(0.0).upper(1.0);
            lambdas.add(lam);
        }

        // z[s] 二进制，选择哪一段（只允许一个段被选中）
        List<Variable> segZ = new ArrayList<>(sCount);
        for (int s = 0; s < sCount; s++) {
            Variable z = model.addVariable("seg_" + s).binary();
            segZ.add(z);
        }

        // ∑ λ[i] = 1
        Expression sumLam = model.addExpression("sum_lambda");
        for (Variable lam : lambdas) sumLam.set(lam, 1.0);
        sumLam.level(1.0);

        // Ha = ∑ H[i] * λ[i]
        Expression defHa = model.addExpression("Ha_by_lambda").set(Ha, -1.0);
        for (int i = 0; i < n; i++) defHa.set(lambdas.get(i), reservoirStorageLine.get(i).getCode());
        defHa.level(0.0);

        // Va = ∑ V[i] * λ[i]
        Expression defVa = model.addExpression("Va_by_lambda").set(Va, -1.0);
        for (int i = 0; i < n; i++) defVa.set(lambdas.get(i), reservoirStorageLine.get(i).getValue() * 1e6);
        defVa.level(0.0);

        // ∑ z[s] = 1 （只选一个 segment）
        Expression sumZ = model.addExpression("sum_seg");
        for (Variable z : segZ) sumZ.set(z, 1.0);
        sumZ.level(1.0);

        // 绑定 λ 到相邻 segment：确保 λ 只有被选中的段的两个端点能取正
        // 端点规则：
        //  λ[0]      ≤ z[0]
        //  λ[i]      ≤ z[i-1] + z[i], i=1..n-2
        //  λ[n-1]    ≤ z[n-2]
        model.addExpression("lam_0_link").set(lambdas.get(0), 1.0).set(segZ.get(0), -1.0).upper(0.0);
        for (int i = 1; i <= n - 2; i++) {
            Expression link = model.addExpression("lam_" + i + "_link");
            link.set(lambdas.get(i), 1.0);
            link.set(segZ.get(i - 1), -1.0);
            link.set(segZ.get(i), -1.0);
            link.upper(0.0);
        }
        model.addExpression("lam_last_link").set(lambdas.get(n - 1), 1.0).set(segZ.get(n - 2), -1.0).upper(0.0);
    }

    /**
     * 构建 “Ha = Σ H[i]*λ[i]、Σλ=1” 的分段线性化骨架，并用二进制 z[s] 限制 λ 只在一个相邻段内非零。
     * 返回 PiecewiseLambdas，以便后续把其他量（Va, Lc …）用同一套 λ 绑定。
     * <p>
     * 约束：
     * (1) Σ λ[i] = 1
     * (2) Ha = Σ H[i]·λ[i]
     * (3) Σ z[s] = 1
     * (4) λ[0] ≤ z[0]; λ[i] ≤ z[i-1]+z[i]; λ[n-1] ≤ z[n-2]
     * <p>
     * 要求 Hgrid 严格递增，长度 ≥ 2。
     */
    public static PiecewiseLambdas addPiecewiseHa(ExpressionsBasedModel model, Variable Ha, double[] Hgrid) {
        if (Hgrid == null || Hgrid.length < 2) {
            throw new IllegalArgumentException("Hgrid 至少需要两个点。");
        }
        // 检查递增
        for (int i = 1; i < Hgrid.length; i++) {
            if (!(Hgrid[i] > Hgrid[i - 1])) {
                throw new IllegalArgumentException("Hgrid 必须严格递增: " + Arrays.toString(Hgrid));
            }
        }
        final int n = Hgrid.length;
        final int sCount = n - 1;

        // λ[i] ∈ [0,1]
        List<Variable> lambdas = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Variable lam = model.addVariable("lam_" + i).lower(0.0).upper(1.0);
            lambdas.add(lam);
        }

        // 段选择 z[s] ∈ {0,1}
        List<Variable> zList = new ArrayList<>(sCount);
        for (int s = 0; s < sCount; s++) {
            Variable z = model.addVariable("seg_" + s).binary();
            zList.add(z);
        }

        // Σ λ = 1
        Expression sumLam = model.addExpression("sum_lambda");
        for (Variable lam : lambdas) sumLam.set(lam, 1.0);
        sumLam.level(1.0);

        // Ha = Σ H[i]*λ[i]
        Expression defHa = model.addExpression("Ha_by_lambda").set(Ha, -1.0);
        for (int i = 0; i < n; i++) defHa.set(lambdas.get(i), Hgrid[i]);
        defHa.level(0.0);

        // Σ z = 1  （只选一个 segment）
        Expression sumZ = model.addExpression("sum_seg");
        for (Variable z : zList) sumZ.set(z, 1.0);
        sumZ.level(1.0);

        // 限制 λ 只能落在被选中的相邻两端点
        // λ[0] ≤ z[0]
        model.addExpression("lam_0_link").set(lambdas.get(0), 1.0).set(zList.get(0), -1.0).upper(0.0);

        // λ[i] ≤ z[i-1] + z[i]  (i=1..n-2)
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
     * Lc = Σ Lc[i]*λ[i]（Lc 为耗水率值）
     */
    public static void linkLinearByLambda(ExpressionsBasedModel model, Variable target, PiecewiseLambdas lam, double[] coefOnSameHGrid) {
        if (coefOnSameHGrid == null || coefOnSameHGrid.length != lam.size()) {
            throw new IllegalArgumentException("coef 长度必须与 λ 数量一致，且与同一套 Hgrid 对齐。");
        }
        Expression expr = model.addExpression(target.getName() + "_by_lambda")
                .set(target, -1.0);
        for (int i = 0; i < lam.size(); i++) {
            expr.set(lam.lambdas.get(i), coefOnSameHGrid[i]);
        }
        expr.level(0.0);
    }

    /**
     * McCormick 双线性松弛：Z = X * Y 的线性包络
     * 给出 Z, X, Y 变量与它们的上下界，添加 4 条不等式。
     */
    private static void addMcCormickBilinear(ExpressionsBasedModel model,
                                             Variable Z, Variable X, Variable Y,
                                             double xL, double xU, double yL, double yU) {

        // 1) Z ≥ xL*Y + yL*X - xL*yL
        Expression mc1 = model.addExpression("mc1")
                .set(Z, 1e3).set(Y, -xL).set(X, -yL);
        mc1.lower(-xL * yL);

        // 2) Z ≥ xU*Y + yU*X - xU*yU
        Expression mc2 = model.addExpression("mc2")
                .set(Z, 1e3).set(Y, -xU).set(X, -yU);
        mc2.lower(-xU * yU);

        // 3) Z ≤ xU*Y + yL*X - xU*yL
        Expression mc3 = model.addExpression("mc3")
                .set(Z, 1e3).set(Y, -xU).set(X, -yL);
        mc3.upper(-xU * yL);

        // 4) Z ≤ xL*Y + yU*X - xL*yU
        Expression mc4 = model.addExpression("mc4")
                .set(Z, 1e3).set(Y, -xL).set(X, -yU);
        mc4.upper(-xL * yU);
    }

    public static Variable addVar(ExpressionsBasedModel m, String name, Double lo, Double hi) {
        Variable v = m.addVariable(name);
        if (lo != null && Double.isFinite(lo) && lo > -1e300) v.lower(lo);
        // 只在上界是“有限且合理”的时候设置；否则不设上界更安全
        if (hi != null && Double.isFinite(hi) && hi < 1e300) v.upper(hi);
        return v;
    }


}
