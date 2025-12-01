package com.hust.generatingcapacity.model.generation.calculate;

import com.hust.generatingcapacity.model.common.Either;
import com.hust.generatingcapacity.model.common.Range;
import com.hust.generatingcapacity.model.generation.dispatch.ConstraintEnvBuilder;
import com.hust.generatingcapacity.model.generation.dispatch.ElasticInfeasibleDiag;
import com.hust.generatingcapacity.model.generation.domain.CodeValue;
import com.hust.generatingcapacity.model.generation.domain.ConstraintData;
import com.hust.generatingcapacity.model.generation.domain.StationData;
import com.hust.generatingcapacity.model.generation.type.ParamType;
import com.hust.generatingcapacity.model.generation.type.PreConditionType;
import com.hust.generatingcapacity.model.generation.util.DisplayUtils;
import com.hust.generatingcapacity.model.generation.vo.*;
import com.hust.generatingcapacity.tools.TimeUtils;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.structure.Structure1D;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class RuleOptimalCal {

    //变量优先级排序（与影响变量的参数优先级一致）
    static Map<Variable, ParamType> varPriorityMap = new HashMap<>();

    public static CalculateStep run(CalculateVO calculateVO) {
        CalculateStep data = calculateVO.getCalStep();
        CalculateParam calParam = calculateVO.getCalParam();
        StationData stationData = calculateVO.getStationData();
        System.out.println("\n开始计算电站 " + calParam.getStation() + " 时段 " + TimeUtils.formatDate(data.getTime()) + " 的发电能力，" + "初始水位为：" + data.getLevelBef() + ",入库径流为：" + data.getInFlow());
        try {
            //调度情景
            Map<String, Object> condition = new ConstraintEnvBuilder().conditionBuild(TimeUtils.getSpecificDate(data.getTime()).get("月"), data.getLevelBef(), calParam.getSchedulingL(), calParam.getPeriod(), data.getInFlow());
            // 1. 初算：使用完整约束
            Map<ParamType, BoundPair> initialBound = getFirstParamBound(stationData, condition);
            Either<CalculateStep, ExpressionsBasedModel> step = calculate(data, calParam, stationData, initialBound);
            // 不需要修正，直接返回
            if (step.isLeft()) {
                return step.getLeft();
            }
            // 2. 需要修正：按优先级逐步放宽约束，直到不需要修正或轮次耗尽
            final int typeCount = ParamType.values().length;
            final int maxRounds = typeCount * 2;  // 软 + 硬 两轮
            StringBuilder remarkBuilder = new StringBuilder();// 记录放宽/违反约束
            for (int round = 1; round <= maxRounds; ) {
                //首先尝试放宽某个参数的约束
                ExpressionsBasedModel infeasibleModel = step.getRight();
                Map.Entry<Variable, String> relaxedVar;
                try {
                    relaxedVar = getRelaxedParamBound(data, infeasibleModel, stationData, initialBound);// 放宽约束主函数
                } catch (Exception e) {//找不到需要放宽的参数则丢给规程边界模型
                    return RuleBasedCal.run(calculateVO);
                }
                ParamType relaxType = varPriorityMap.get(relaxedVar.getKey());
                ConstraintData firestViolatedCon = ConstraintData.getFirstViolatedConstraint(stationData.getConstraints(), condition, relaxType, relaxedVar.getValue());
                Either<CalculateStep, ExpressionsBasedModel> nextStep = calculate(data, calParam, stationData, initialBound);
                if (nextStep.isLeft()) {
                    //记录放宽信息
                    remarkBuilder.append("约束：").append(firestViolatedCon == null ? "" : firestViolatedCon.getDescription()).append(" 适当放宽，")
                            .append("参数：").append(relaxType.toString()).append("取值区间被调整为：").append(initialBound.get(relaxType).toString()).append("\n");
                    nextStep.getLeft().setRemark(remarkBuilder.toString());
                    return nextStep.getLeft();
                } else {//开始放弃某些约束
                    Map<ParamType, BoundPair> reviseBound = getRevisionParamBound(stationData, condition, firestViolatedCon, relaxType, round);
                    Either<CalculateStep, ExpressionsBasedModel> reviseStep = calculate(data, calParam, stationData, reviseBound);
                    if (reviseStep.isLeft()) {
                        //记录放弃约束信息
                        remarkBuilder.append("为保障发电计算模型有解，该项约束：").append(firestViolatedCon == null ? "" : firestViolatedCon.getDescription()).append("被放弃，")
                                .append("参数：").append(relaxType.toString()).append("取值区间被调整为：").append(reviseBound.get(relaxType).toString()).append("\n");
                        reviseStep.getLeft().setRemark(remarkBuilder.toString());
                        return reviseStep.getLeft();
                    } else {
                        round++;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 3. 全部放宽轮次耗尽，仍然需要修正/内部有报错信息 → 采用规程边界模型
        return RuleBasedCal.run(calculateVO);
    }

    /**
     * 计算单步
     *
     * @param data
     * @param calParam
     * @param stationData
     * @param cound
     * @return
     */
    public static Either<CalculateStep, ExpressionsBasedModel> calculate(CalculateStep data, CalculateParam calParam, StationData stationData, Map<ParamType, BoundPair> cound) {
        System.out.println("此阶段参数边界：" + cound.toString());
        // 计算所需初始运行数据
        double Qin = data.getInFlow();
        double Hb = data.getLevelBef();
        double Vb = CodeValue.linearInterpolation(Hb, stationData.getReservoirStorageLine()) * 1e6;//m³
        int t = calParam.getPeriod();
        // 降采样水位边界，提高计算速度
        double dHCound;
        if (stationData.getReservoirStorageLine().isEmpty()) {
            dHCound = cound.get(ParamType.H).getMaxVal() - cound.get(ParamType.H).getMinVal();
        } else {
            dHCound = CodeValue.codeDifference(Vb / 1e6, (Vb + Qin * t) / 1e6, stationData.getReservoirStorageLine());
        }
        double H_min = Math.min(cound.get(ParamType.H).getMinVal(), Math.round(Hb)) - Math.min(dHCound, 10.0);
        double H_max = Math.max(cound.get(ParamType.H).getMaxVal(), Math.round(Hb)) + Math.min(dHCound, 10.0);
        //获取特征曲线
        List<CodeValue> reservoirStorageLine = stationData.getReservoirStorageLine()
                .stream()
                .filter(cv -> cv.getCode() >= H_min && cv.getCode() <= H_max)
                .collect(Collectors.toList());
        reservoirStorageLine = CodeValue.enlargeStep(reservoirStorageLine, 2);
        //耗水率
        List<CodeValue> waterConsumptionLine = stationData.getWaterConsumptionLine().size() > 1 ?
                stationData.getWaterConsumptionLine().stream()
                        .filter(cv -> cv.getCode() >= H_min && cv.getCode() <= H_max)
                        .collect(Collectors.toList()) :
                stationData.getWaterConsumptionLine();

        //重新设置流量的上边界
        double dV_max = CodeValue.difference(Hb, H_min, reservoirStorageLine) * 1e6;
        double Q_max = Math.min(cound.get(ParamType.Qo).getMaxVal(), (Qin * t - dV_max) / t);
        cound.get(ParamType.Qp).setMaxVal(Q_max);
        cound.get(ParamType.Qo).setMaxVal(Q_max);

        // 计算变量边界
        double Vp_min = cound.get(ParamType.Qp).getMinVal() * t;//m³
        double Vp_max = cound.get(ParamType.Qp).getMaxVal() * t;//m³
        double L_min = CodeValue.getMinValue(waterConsumptionLine) * 1e3;//m³/WM*H
        double L_max = CodeValue.getMaxValue(waterConsumptionLine) * 1e3;//m³/WM*H
        double genL = Math.max(cound.get(ParamType.P).getMinVal() * t / 3600, Vp_min / L_max);//WM*H
        double genU = Math.min(cound.get(ParamType.P).getMaxVal() * t / 3600, Vp_max / L_min);//WM*H
        System.setProperty("shut.up.ojAlgo", "true");
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // 发电流量
        Variable Qp = addVar(model, "Qp", cound.get(ParamType.Qp).getMinVal(), cound.get(ParamType.Qp).getMaxVal());
        varPriorityMap.put(Qp, ParamType.Qp);
        // 发电水量 Vp
        Variable Vp = addVar(model, "Vp", Vp_min, Vp_max);
        varPriorityMap.put(Vp, ParamType.Qp);
        // 出库流量
        Variable Qo = addVar(model, "Qo", cound.get(ParamType.Qo).getMinVal(), cound.get(ParamType.Qo).getMaxVal());
        varPriorityMap.put(Qo, ParamType.Qo);
        // 时段末水位
        Variable Ha = addVar(model, "Ha", cound.get(ParamType.H).getMinVal(), cound.get(ParamType.H).getMaxVal());
        varPriorityMap.put(Ha, ParamType.H);
        // 末水位对应绝对库容（决策变量）
        Variable Va = addVar(model, "Va", CodeValue.linearInterpolation(cound.get(ParamType.H).getMinVal(), reservoirStorageLine) * 1e6, CodeValue.linearInterpolation(cound.get(ParamType.H).getMaxVal(), reservoirStorageLine) * 1e6);
        varPriorityMap.put(Va, ParamType.H);
        // 水位变幅
        Variable dH = addVar(model, "dH", cound.get(ParamType.dH).getMinVal(), cound.get(ParamType.dH).getMaxVal());
        varPriorityMap.put(dH, ParamType.dH);
        // 水量变幅 dV（水位库容曲线为百万）
        Variable dV = addVar(model, "dV", CodeValue.difference(Hb, cound.get(ParamType.H).getMinVal(), reservoirStorageLine) * 1e6, CodeValue.difference(Hb, cound.get(ParamType.H).getMaxVal(), reservoirStorageLine) * 1e6);
        varPriorityMap.put(dV, ParamType.H);
        // 发电负荷
        Variable Gen = addVar(model, "Gen", genL, genU);
        varPriorityMap.put(Gen, ParamType.P);

        // ===== 添加约束 =====
        // ===== 注意等式左侧为变量，右侧为常数值 =====
        // 1. Qp ≤ Qo （发电流量不超过总出库流量）
        Expression constr1 = model.addExpression("Qp_le_Qo")
                .set(Qp, 1.0)    // 系数 * Qp
                .set(Qo, -1.0);  // 系数 * Qo  => 表达式 = Qp - Qo
        constr1.upper(0.0);       // 约束: Qp - Qo ≤ 0  【即 Qp ≤ Qo】

        // 2. 水位变幅定义: dH = Ha - Hb
        Expression constr2 = model.addExpression("dH_def")
                .set(Ha, 1.0)
                .set(dH, -1.0);
        constr2.level(Hb);        // 约束: Ha - dH = Hb  【即 dH = Ha - Hb】

        // 3. 水量变幅定义: Vb = Va - dV
        Expression constr3 = model.addExpression("dV_def")
                .set(dV, -1.0)
                .set(Va, 1.0);
        constr3.level(Vb); //约束： Va - dV = Vb  【即 dV = Va - Vb】

        // 4. 发电水量与流量关系: Vp = Qp * t
        Expression constr4 = model.addExpression("Vp_def")
                .set(Vp, 1)
                .set(Qp, -t);
        constr4.level(0.0);       // 约束: Vp - t*Qp = 0  【即 Vp = Qp * t】

        // 5. 出库水量与流量关系: dV = (Qin-Qo) * t
        Expression constr5 = model.addExpression("dV_def_Q")
                .set(dV, 1)
                .set(Qo, t);
        constr5.level(Qin * t); // 约束: dV + Qo*t = Qin*t

        // 弃水软惩罚
        double eps = calParam.isGenMin() ? 0.6 : 0.01; // 计算最小发电量时，弃水约束应增大权重
        Variable S_spill = addVar(model, "S_spill", 0.0, cound.get(ParamType.Qo).getMaxVal());
        Expression spill_def1 = model.addExpression("spill_ge_Qo_minus_Qp")
                .set(S_spill, 1.0).set(Qo, -1.0).set(Qp, 1.0);
        spill_def1.lower(0.0); // S_spill - Qo + Qp >= 0  -> S_spill >= Qo - Qp


        // 发电计算
        Variable Lc;
        double L;
        if (calParam.isConsiderH()) {//有细致的耗水率曲线
            // 耗水率
            Lc = addVar(model, "Lc", CodeValue.getMinValue(waterConsumptionLine) * 1e3, CodeValue.getMaxValue(waterConsumptionLine) * 1e3);
            CodeValue.sampleToGrid(reservoirStorageLine, waterConsumptionLine);// 重采样到同一网格
            double[] Hgrid = reservoirStorageLine.stream().mapToDouble(CodeValue::getCode).toArray();
            PiecewiseLambdas lam = addPiecewiseHa(model, Ha, Hgrid); // 构建 Ha 分段线性化骨架
            double[] Lgrid = waterConsumptionLine.stream().mapToDouble(cv -> cv.getValue() * 1e3).toArray();
            linkLinearByLambda(model, Lc, lam, Lgrid); // 把 Lc 用同一套 λ 绑定
            double[] Vgrid = reservoirStorageLine.stream().mapToDouble(cv -> cv.getValue() * 1e6).toArray();
            linkLinearByLambda(model, Va, lam, Vgrid); // 把 Va 用同一套 λ 绑定
            // McCormick：Vp = Gen * L耗
            addMcCormickBilinear(model, Vp, Gen, Lc, genL, genU, L_min, L_max);
        } else {
            L = waterConsumptionLine.get(0).getValue() * 1e3;
            double[] Hgrid = reservoirStorageLine.stream().mapToDouble(CodeValue::getCode).toArray();
            PiecewiseLambdas lam = addPiecewiseHa(model, Ha, Hgrid); // 构建 Ha 分段线性化骨架
            double[] Vgrid = reservoirStorageLine.stream().mapToDouble(cv -> cv.getValue() * 1e6).toArray();
            linkLinearByLambda(model, Va, lam, Vgrid); // 把 Va 用同一套 λ 绑定
            // 6. 发电能力与耗水率/发电流量的关系
            Expression constr6 = model.addExpression("Gen_def")
                    .set(Vp, 1)
                    .set(Gen, -L);
            constr6.level(0.0);
        }

        // === 目标：最大化 Gen ===
        if (calParam.isGenMin()) {
            Gen.weight(-1.0);
        } else {
            Gen.weight(1.0);
        }
        // 目标：轻微惩罚弃水
        S_spill.weight(-eps);

        // 求解
        model.options.time_abort = 60000;    // 运行 1 分钟后强制停止
        model.options.iterations_abort = 1000000; // 达到最大迭代数后停止
        Optimisation.Result rs = model.maximise();

        // 存储结果
//        if (rs.getState().equals(Optimisation.State.INFEASIBLE)){//人工分析无解原因
//            InfeasibleBoundCheck.analyse(model);
//        }
//        data.setRemark(String.valueOf(rs.getState()));//记录此次运算状态
        data.setRevise(!rs.getState().isFeasible());//记录是否需要修正
        data.setLevelAft(Ha.getValue().doubleValue());
        data.setQp(Qp.getValue().doubleValue());
        data.setQo(Qo.getValue().doubleValue());
        data.setCalGen(Gen.getValue().doubleValue());
        //有解则返回解，无解则返回模型
        if (rs.getState().isFeasible()) return Either.left(data);
        else return Either.right(model);
    }

    /**
     * 获取参数边界
     *
     * @param stationData
     * @return
     */
    public static Map<ParamType, BoundPair> getFirstParamBound(StationData stationData, Map<String, Object> condition) {
        //获取参数边界
        List<ConstraintData> constraints = stationData.getConstraints();
        Map<ParamType, BoundPair> cound = stationData.setInitialBoundPair();
        for (ConstraintData constraint : constraints) {
            if (constraint.isConditionActive(constraint.getCondition(), condition)) {
                List<String> paramList = constraint.getParam();
                new ConstraintData().getParamBoundPair(paramList, condition, cound);
            }
        }
        return cound;
    }

    /**
     * 放宽某个参数的约束并且返回该参数类型
     *
     * @param data
     * @param model
     * @param stationData
     * @param initialBound
     * @return
     */
    public static Map.Entry<Variable, String> getRelaxedParamBound(CalculateStep data, ExpressionsBasedModel model, StationData stationData, Map<ParamType, BoundPair> initialBound) {
        System.out.println("主模型不可行，开始用松弛变量法诊断矛盾约束...");
        ElasticInfeasibleDiag.SlackInfo s = ElasticInfeasibleDiag.diagnose(model);
        Expression expression = model.getExpression(s.constraintName);
        // 拿到这条表达式中，所有线性变量及参数
        Set<Map.Entry<Structure1D.IntIndex, BigDecimal>> entries = expression.getLinearEntrySet();
        Map<Variable, BigDecimal> values = new HashMap<>();
        for (Map.Entry<Structure1D.IntIndex, BigDecimal> entry : entries) {
            Structure1D.IntIndex key = entry.getKey();   // 变量索引
            BigDecimal coef = entry.getValue();          // 系数（正负都在这）
            Variable var = model.getVariable(key);       // 索引 -> Variable
            values.put(var, coef);
        }
        Optional<Map.Entry<Variable, BigDecimal>> priorityVar = varPriorityMap.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(
                        e2.getValue().getPriority(),
                        e1.getValue().getPriority()))
                .filter(e -> values.containsKey(e.getKey()))
                .map(e -> Map.entry(e.getKey(), values.get(e.getKey())))
                .findFirst();
        if (priorityVar.isEmpty()) {
            throw new RuntimeException("无法定位矛盾约束中的优先级变量！");
        }
        ParamType priorityType = varPriorityMap.get(priorityVar.get().getKey());
        boolean isUpper = s.side.contains("upper");
        boolean isPositive = priorityVar.get().getValue().compareTo(BigDecimal.ZERO) > 0;
        String change = (isUpper == isPositive) ? "下界放宽" : "上界放宽";
        double slackValue = change.equals("下界放宽") ? -1 * s.value : s.value;//下界放宽即为减去松弛量
        double changeValue = getChangeValue(priorityVar.get().getKey(), slackValue, new Range<>(s.lo, s.hi), priorityVar.get().getValue().abs().doubleValue(), stationData, data);//计算变量变化值
        changeValue = changeValue * 1.2;//适当多放宽
        BoundPair boundPair = initialBound.get(priorityType);
        if (change.equals("上界放宽")) {
            boundPair.setMaxVal(boundPair.getMaxVal() + changeValue);
        } else {
            boundPair.setMinVal(boundPair.getMinVal() + changeValue);
        }
        initialBound.put(priorityType, boundPair);
        System.out.println("放宽参数为：" + priorityType + "，变量：" + priorityVar.get().getKey().getName() + "，变化值：" + String.format("%.4f", changeValue) + "，" + change);
        return Map.entry(priorityVar.get().getKey(), change);
    }

    /**
     * 计算变量变化值
     *
     * @param var         变量
     * @param slackValue  松弛变量值
     * @param varRange    变量取值范围
     * @param coef        变量前系数（绝对值）
     * @param stationData
     * @param calStep
     * @return 变量变化值(正、负)
     */
    public static double getChangeValue(Variable var, double slackValue, Range<Double> varRange, double coef, StationData stationData, CalculateStep calStep) {
        String varName = var.getName();
        List<CodeValue> reservoirStorageLine = stationData.getReservoirStorageLine()
                .stream()
                .map(r -> new CodeValue(r.getCode(), r.getValue() * 1e6))
                .collect(Collectors.toList());
        double changeValue = 0.0;
        switch (varName) {
            case "Qp":
            case "Qo":
            case "Ha":
            case "dH":
            case "Gen":
            case "Vp":
                changeValue = slackValue / coef;
                break;
            case "Va":
                double Va_min = varRange.getStart();
                double Va_max = varRange.getEnd();
                double H_min = CodeValue.codeLinearInterpolation(Va_min, reservoirStorageLine);
                double H_max = CodeValue.codeLinearInterpolation(Va_max, reservoirStorageLine);
                double Va_change = slackValue >= 0 ? Va_max + slackValue : Va_min + slackValue;//slackValue >= 0 证明上界需要放宽
                double H_change = CodeValue.codeLinearInterpolation(Va_change, reservoirStorageLine);
                changeValue = slackValue >= 0 ? H_change - H_max : H_change - H_min;
                break;
            case "dV":
                double dV_min = varRange.getStart();
                double dV_max = varRange.getEnd();
                double Hb = calStep.getLevelBef();
                double H_dV_min = CodeValue.codeLinearInterpolation(dV_min + CodeValue.linearInterpolation(Hb, reservoirStorageLine), reservoirStorageLine);
                double H_dV_max = CodeValue.codeLinearInterpolation(dV_max + CodeValue.linearInterpolation(Hb, reservoirStorageLine), reservoirStorageLine);
                double dV_change = slackValue >= 0 ? dV_max + slackValue : dV_min + slackValue;//slackValue >= 0 证明上界需要放宽
                double H_dV_change = CodeValue.codeLinearInterpolation(dV_change + CodeValue.linearInterpolation(Hb, reservoirStorageLine), reservoirStorageLine);
                changeValue = slackValue >= 0 ? H_dV_change - H_dV_max : H_dV_change - H_dV_min;
                break;
            default:
                changeValue = slackValue;
        }
        return changeValue;
    }

    /**
     * 获取约束排序后的边界
     *
     * @param stationData
     * @return
     */
    public static Map<ParamType, BoundPair> getRevisionParamBound(StationData stationData, Map<String, Object> condition, ConstraintData firstViolatedCon, ParamType paramType, int sorted) {
        //获取参数边界
        List<ConstraintData> constraints = stationData.getConstraints();
        List<String> paramExprList = new ArrayList<>();
        if (firstViolatedCon == null) {//需要调整的参数没有找到对应的约束
            List<ConstraintData> rigidConstraints = constraints.stream().filter(ConstraintData::getRigid).toList();
            List<ConstraintData> nonRigidConstraints = constraints.stream().filter(c -> !c.getRigid()).toList();
            //本次剔除哪种参数约束
            int typeCount = ParamType.values().length;
            Set<ParamType> excludedTypes;
            if (sorted <= typeCount) {
                // 只剔除软约束中的低优先级参数
                excludedTypes = buildExcludedTypes(sorted, typeCount);
            } else {
                // 不再考虑软约束，开始在硬约束中剔除低优先级参数
                int rigidSorted = sorted - typeCount;
                excludedTypes = buildExcludedTypes(rigidSorted, typeCount);
            }
            //收集本轮生效的参数表达式
            if (sorted <= typeCount) {
                // 阶段一：只对软约束做“按优先级剔除”
                collectActiveParams(nonRigidConstraints, condition, excludedTypes, paramExprList);
                // 硬约束全保留
                collectActiveParams(rigidConstraints, condition, Set.of(), paramExprList);
            } else {
                // 阶段二：软约束不再考虑，只在硬约束里按优先级剔除
                collectActiveParams(rigidConstraints, condition, excludedTypes, paramExprList);
            }
        } else {//先按照违例约束剔除
            // 违例约束剔除其参数
            collectActiveParams(List.of(firstViolatedCon), condition, Set.of(paramType), paramExprList);
            constraints.remove(firstViolatedCon);
            // 其余约束全保留
            collectActiveParams(constraints, condition, Set.of(), paramExprList);
        }
        //初始边界
        Map<ParamType, BoundPair> cound = stationData.setInitialBoundPair();
        //获取参数边界
//        System.out.println("本轮生效的参数类型：" + paramExprList);
        new ConstraintData().getParamBoundPair(paramExprList, condition, cound);
        return cound;
    }

    /**
     * 根据“第几轮 sorted”和 ParamType 总数，计算本轮需要剔除的 ParamType 集合。
     * 当前规则：priority 越大，优先级越低，越早被剔除。
     */
    private static Set<ParamType> buildExcludedTypes(int sorted, int totalTypes) {
        if (sorted <= 0) {
            return Set.of();
        }
        int threshold = totalTypes - sorted;
        return Arrays.stream(ParamType.values())
                .filter(p -> p.getPriority() >= threshold)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ParamType.class)));
    }

    /**
     * 从一批约束中，收集满足 condition 且其参数类型不在 excludedTypes 里的表达式。
     */
    private static void collectActiveParams(List<ConstraintData> constraints, Map<String, Object> conditionEnv, Set<ParamType> excludedTypes, List<String> result) {
        for (ConstraintData constraint : constraints) {
            if (!constraint.isConditionActive(constraint.getCondition(), conditionEnv)) {
                continue;
            }
            for (String exp : constraint.getParam()) {
                String paramName = DisplayUtils.getMessageFromExp(exp, "param");
                ParamType type;
                try {
                    type = ParamType.valueOf(paramName);
                } catch (IllegalArgumentException e) {
                    // 非法的参数名，直接跳过，避免整个过程挂掉
                    continue;
                }
                if (!excludedTypes.contains(type)) {
                    result.add(exp);
                }
            }
        }
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
                .set(Z, 1).set(Y, -xL).set(X, -yL);
        mc1.lower(-xL * yL);

        // 2) Z ≥ xU*Y + yU*X - xU*yU
        Expression mc2 = model.addExpression("mc2")
                .set(Z, 1).set(Y, -xU).set(X, -yU);
        mc2.lower(-xU * yU);

        // 3) Z ≤ xU*Y + yL*X - xU*yL
        Expression mc3 = model.addExpression("mc3")
                .set(Z, 1).set(Y, -xU).set(X, -yL);
        mc3.upper(-xU * yL);

        // 4) Z ≤ xL*Y + yU*X - xL*yU
        Expression mc4 = model.addExpression("mc4")
                .set(Z, 1).set(Y, -xL).set(X, -yU);
        mc4.upper(-xL * yU);
    }

    /**
     * 设置参数变量及其上下界
     *
     * @param m
     * @param name
     * @param lo
     * @param hi
     * @return
     */
    public static Variable addVar(ExpressionsBasedModel m, String name, Double lo, Double hi) {
        Variable v = m.addVariable(name);
        if (lo != null && Double.isFinite(lo) && lo > -1e300) v.lower(lo);
        // 只在上界是“有限且合理”的时候设置；否则不设上界更安全
        if (hi != null && Double.isFinite(hi) && hi < 1e300) v.upper(hi);
        return v;
    }
}
