package com.hust.generatingcapacity.model.generation.calculate;

import com.hust.generatingcapacity.model.generation.dispatch.ConstraintEnvBuilder;
import com.hust.generatingcapacity.model.generation.domain.*;
import com.hust.generatingcapacity.model.generation.type.ParamType;
import com.hust.generatingcapacity.model.generation.type.PreConditionType;
import com.hust.generatingcapacity.model.generation.vo.*;
import com.hust.generatingcapacity.tools.TimeUtils;

import java.util.*;

public class RuleBasedCal {

    public static CalculateStep run(CalculateVO calculateVO) {
        CalculateStep data = calculateVO.getCalStep();
        CalculateParam calParam = calculateVO.getCalParam();
        StationData stationData = calculateVO.getStationData();
        //将水库的特征信息作为常生效的约束条件
        Map<ParamType, BoundPair> initialBoundPair = stationData.setInitialBoundPair();
        if (initialBoundPair != null && !initialBoundPair.isEmpty()) {
            List<String> param = new LinkedList<>();
            for (Map.Entry<ParamType, BoundPair> entry : initialBoundPair.entrySet()) {
                BoundPair boundPair = entry.getValue();
                param.add(boundPair.toParamMinString(calParam.getPeriod()));
                param.add(boundPair.toParamMaxString(calParam.getPeriod()));
            }
            String condition = "dL >= 1"; // 恒真条件
            ConstraintData constraintData = new ConstraintData();
            constraintData.setConstraintType("水库特征约束");
            constraintData.setRigid(true);
            constraintData.setDescription("水库特征约束");
            constraintData.setCondition(condition);
            constraintData.setParam(param);
            stationData.getConstraints().add(constraintData);
        }
        // ——配置——
        final int MAX_ATTEMPTS = 6;
        final int CONFLICT_WINDOW = 3; // 最近N次用于冲突判断
        // ——状态——
        CalculateStep curr = data;
        ParamValue lastViolation = null;            // 本轮判定出的“最严重违反”
        final Deque<ParamType> history = new ArrayDeque<>(); // 记录最近的违反类型（保留顺序）
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            final boolean isRevision = (attempt > 0);
            curr.setRevise(isRevision);
            // 1) 调度一步
            final CalculateStep next = RuleBasedCal.calculate(curr, calParam, lastViolation, stationData);
            // 2) 评估违反参数集合
            final List<ParamValue> violations = getParamMap(curr, next, calParam, stationData);
            // ——无违反：收工——
            if (violations.isEmpty()) {
                next.setRemark(buildWarnMessage(history, lastViolation, null, CONFLICT_WINDOW));
                return next;
            }
            // 3) 选“最严重”的一个作为修正方向，记入历史
            lastViolation = selectMostSevereParam(violations);
            history.addLast(lastViolation.getParamType());
            while (history.size() > CONFLICT_WINDOW) { // 只保留最近窗口
                history.removeFirst();
            }
            // 4) 是否需要继续？
            if (!shouldContinue(attempt, MAX_ATTEMPTS, lastViolation)) {
                next.setRemark(buildWarnMessage(history, lastViolation, violations, CONFLICT_WINDOW));
                return next;
            }
            // 5) 进入下一轮
            curr = next;
        }
        // ——兜底（理论到不了这儿）——
        curr.setRemark(buildWarnMessage(history, lastViolation, Collections.emptyList(), CONFLICT_WINDOW));
        return curr;
    }

    /**
     * 获取被违反的约束参数
     *
     * @param data
     * @param data_aft
     * @param calParam
     * @param stationData
     * @return
     */
    private static List<ParamValue> getParamMap(CalculateStep data, CalculateStep data_aft, CalculateParam calParam, StationData stationData) {
        //约束条件
        Integer T = TimeUtils.getSpecificDate(data.getTime()).get("月");
        double H = data.getLevelBef();
        double Qin = data.getInFlow();
        Map<String, Object> conditionEnv = new ConstraintEnvBuilder().conditionBuild(T, H, calParam.getSchedulingL(), calParam.getPeriod(), Qin);
        //约束参数
        double H_aft = data_aft.getLevelAft();
        double dH = H_aft - H;
        Map<String, Object> paramEnv = new ConstraintEnvBuilder().paramBuild(H_aft, dH, data_aft.getQp(), data_aft.getQo(), 0, data.getCalGen());
        //检查约束
        List<ParamValue> result = new ArrayList<>();
        List<ConstraintData> constraints = stationData.getConstraints();
        for (ConstraintData constraint : constraints) {
            if (constraint.isConditionActive(constraint.getCondition(), conditionEnv)) {
                List<String> paramList = constraint.getParam();
                Map<ParamType, Double> param = constraint.getParamConstraintValue(paramList, paramEnv, conditionEnv);
                if (param != null && !param.isEmpty()) {
                    for (Map.Entry<ParamType, Double> entry : param.entrySet()) {
                        ParamValue pv = new ParamValue(entry.getKey(), entry.getValue(), constraint.getDescription(), constraint.getRigid());
                        result.add(pv);
                    }
                }
            }
        }
        return result;
    }

    /**
     * 是否继续迭代的统一判断：上限 + “最高优先级允许再搏一把”的规则
     */
    private static boolean shouldContinue(int attempt, int maxAttempts, ParamValue lastViolation) {
        if (attempt + 1 < maxAttempts) return true; // 还没到上限，继续
        // 已到上限：仅当“最后违反的优先级==1”才允许再试一次（可按你规则改）
        return lastViolation != null
                && lastViolation.getParamType() != null
                && lastViolation.getParamType().getPriority() == 1;
    }

    /**
     * 生成告警信息：最近窗口内若出现多类违反，则提示潜在冲突；同时给出“最后满足/未满足”的描述。
     */
    private static String buildWarnMessage(Deque<ParamType> history, ParamValue lastViolation, List<ParamValue> currentViolations, int window) {
        if (history == null || history.isEmpty()) return "";
        // 去重但保留相对顺序
        LinkedHashSet<ParamType> uniq = new LinkedHashSet<>(history);

        String finalSatisfied = (lastViolation == null) ? "" : safeDesc(lastViolation);
        String currentUnmet = (currentViolations == null || currentViolations.isEmpty()) ? "" : safeDesc(selectMostSevereParam(currentViolations));
        StringBuilder sb = new StringBuilder();
        if (uniq.size() > 1) {
            sb.append("警告：存在约束冲突（最近").append(Math.min(window, history.size()))
                    .append("次出现多类约束）：").append(uniq).append("；");
        }
        if (!finalSatisfied.isEmpty()) {
            sb.append("最后满足的约束：").append(finalSatisfied).append("；");
        }
        if (!currentUnmet.isEmpty()) {
            sb.append("当前未满足的约束：").append(currentUnmet);
        }
        return sb.toString();
    }

    private static String safeDesc(ParamValue pv) {
        try {
            return (pv == null) ? "" : String.valueOf(pv.getDescription());
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 选取最严重的约束参数
     *
     * @param paramList
     * @return
     */
    private static ParamValue selectMostSevereParam(List<ParamValue> paramList) {
        if (paramList == null || paramList.isEmpty()) {
            return null;
        }
        //先判断硬约束，再看约束参数的优先级
        List<ParamValue> filteredList = paramList.stream()
                .sorted(Comparator.comparing(ParamValue::isRigid, Comparator.reverseOrder())
                        .thenComparing((a, b) -> ParamType.comparePriority(a.getParamType(), b.getParamType())))
                .toList();
        return filteredList.get(0);
    }

    /**
     * 基于调度规程边界计算发电能力
     *
     * @param data
     * @param calParam
     * @param paramValue
     * @param stationData
     * @return
     */
    public static CalculateStep calculate(CalculateStep data, CalculateParam calParam, ParamValue paramValue, StationData stationData) {
        CalculateStep calculateStep;
        if (calParam.isGenMin()) {
            calculateStep = calculateMin(data, calParam, paramValue, stationData);
        } else {
            calculateStep = calculateMax(data, calParam, paramValue, stationData);
        }
        return calculateStep;
    }

    /**
     * 计算最小发电能力
     *
     * @param data
     * @param calParam
     * @param paramValue
     * @param stationData
     * @return
     */
    private static CalculateStep calculateMin(CalculateStep data, CalculateParam calParam, ParamValue paramValue, StationData stationData) {
        CalculateStep calculateStep;
        if (!data.isRevise()) {
            calculateStep = calculateMinFirstTime(data, calParam, stationData);
        } else {
            calculateStep = calculateMinRevision(data, calParam, paramValue, stationData);
        }
        return calculateStep;
    }

    /**
     * 初始计算
     *
     * @param data
     * @param calParam
     * @param stationData
     * @return
     */
    private static CalculateStep calculateMinFirstTime(CalculateStep data, CalculateParam calParam, StationData stationData) {
        // 计算 qpMin
        data.setQp(0.0);
        data.setQo(0.0);
        data.setCalGen(0.0);
        double levelAft = calculateLevelAft(data.getLevelBef(), 0.0, data.getInFlow(), calParam, stationData);
        data.setLevelAft(levelAft);
        return data;
    }

    /**
     * 修正计算
     *
     * @param data
     * @param calParam
     * @param paramValue
     * @param stationData
     * @return
     */
    private static CalculateStep calculateMinRevision(CalculateStep data, CalculateParam calParam, ParamValue paramValue, StationData stationData) {
        double rate;
        if (calParam.isConsiderH()) {
            rate = CodeValue.linearInterpolation(stationData.getIsWaterConsumption() ? data.getLevelBef() : data.getHead(), stationData.getWaterConsumptionLine());
        } else {
            rate = stationData.getWaterConsumptionLine().get(0).getValue();
        }
        ParamType type = paramValue.getParamType();
        double value = paramValue.getParamValue();
        switch (type) {
            case Qo -> {
                double qo = value;
                double qp = value;
                applyResult(data, qp, qo, rate, calParam, stationData);
            }
            case Qp -> {
                double qp = value;
                double qo = Math.max(data.getQo(), qp);
                applyResult(data, qp, qo, rate, calParam, stationData);
            }
            case dH -> {
                double H_aft = data.getLevelBef() + value;
                data.setLevelAft(H_aft);
                double qo_cal = calculateQo(H_aft, data, calParam, stationData);
                applyResult(data, qo_cal, qo_cal, rate, calParam, stationData);
            }
            case H -> {
                double H_aft = value;
                data.setLevelAft(H_aft);
                double qo_cal = calculateQo(H_aft, data, calParam, stationData);
                applyResult(data, qo_cal, qo_cal, rate, calParam, stationData);
            }
            case P -> {
                double gen = value;
                double qp = gen * rate / 1e-3 / calParam.getPeriod();
                double qo = data.getQo();
                applyResult(data, qp, qo, rate, calParam, stationData);
            }
            default -> throw new IllegalArgumentException("不支持的参数类型: " + type);
        }
        return data;
    }

    /**
     * 计算最大发电能力
     *
     * @param data
     * @param calParam
     * @param paramValue
     * @param stationData
     * @return
     */
    private static CalculateStep calculateMax(CalculateStep data, CalculateParam calParam, ParamValue paramValue, StationData stationData) {
        if (!data.isRevise()) {
            return calculateMaxFirstTime(data, calParam, stationData);
        } else {
            return calculateMaxRevision(data, calParam, paramValue, stationData);
        }
    }

    /**
     * 初始计算
     *
     * @param data
     * @param calParam
     * @param stationData
     * @return
     */
    private static CalculateStep calculateMaxFirstTime(CalculateStep data, CalculateParam calParam, StationData stationData) {
        if (calParam.isConsiderH()) {
            // 计算 qpMax
            double qpMax = stationData.getNHQLines().stream()
                    .mapToDouble(line -> NHQData.getMaxQ(data.getHead(), line))
                    .sum();
            data.setQp(qpMax);
            data.setQo(qpMax);
            double rate = CodeValue.linearInterpolation(stationData.getIsWaterConsumption() ? data.getLevelBef() : data.getHead(), stationData.getWaterConsumptionLine());
            double gen = qpMax * calParam.getPeriod() / rate / 1e3;
            data.setCalGen(gen);
            double levelAft = calculateLevelAft(data.getLevelBef(), qpMax, data.getInFlow(), calParam, stationData);
            data.setLevelAft(levelAft);
            double headAft = data.getLevelAft() - CodeValue.linearInterpolation(qpMax, stationData.getTailLevelFlowLine());
            data.setHeadAft(headAft);
        } else {
            double genMax = stationData.getInstalledCapacity() * calParam.getPeriod() / 3600;//MW*H
            double rate = stationData.getWaterConsumptionLine().get(0).getValue();
            double qpMax = genMax * rate / 1e-3 / calParam.getPeriod();
            data.setQp(qpMax);
            data.setQo(qpMax);
            data.setCalGen(genMax);
            double levelAft = calculateLevelAft(data.getLevelBef(), qpMax, data.getInFlow(), calParam, stationData);
            data.setLevelAft(levelAft);
        }
        return data;
    }

    /**
     * 修正计算
     *
     * @param data
     * @param calParam
     * @param paramValue
     * @param stationData
     * @return
     */
    private static CalculateStep calculateMaxRevision(CalculateStep data, CalculateParam calParam, ParamValue paramValue, StationData stationData) {
        if (calParam.isConsiderH()) {
            double qpMax = stationData.getNHQLines().stream()
                    .mapToDouble(line -> NHQData.getMaxQ(data.getHead(), line))
                    .sum();
            double rate = CodeValue.linearInterpolation(stationData.getIsWaterConsumption() ? data.getLevelBef() : data.getHead(), stationData.getWaterConsumptionLine());
            ParamType type = paramValue.getParamType();
            double value = paramValue.getParamValue();
            switch (type) {
                case Qo -> {
                    double qo = value;
                    double qp = Math.min(qpMax, qo);
                    applyResult(data, qp, qo, rate, calParam, stationData);
                }
                case Qp -> {
                    double qp = value;
                    double qo = Math.max(data.getQo(), qp);
                    applyResult(data, qp, qo, rate, calParam, stationData);
                }
                case dH -> {
                    double H_aft = data.getLevelBef() + value;
                    data.setLevelAft(H_aft);
                    double qo_cal = calculateQo(H_aft, data, calParam, stationData);
                    double qp_cal = Math.min(qpMax, qo_cal);
                    applyResult(data, qp_cal, qo_cal, rate, calParam, stationData);
                }
                case H -> {
                    double H_aft = value;
                    data.setLevelAft(H_aft);
                    double qo_cal = calculateQo(H_aft, data, calParam, stationData);
                    double qp_cal = Math.min(qpMax, qo_cal);
                    applyResult(data, qp_cal, qo_cal, rate, calParam, stationData);
                }
                case P -> {
                    double gen = value;
                    double qp = gen * rate / 1e-3 / calParam.getPeriod();
                    double qo = data.getQo();
                    applyResult(data, qp, qo, rate, calParam, stationData);
                }
                default -> throw new IllegalArgumentException("不支持的参数类型: " + type);
            }
        } else {
            // 不考虑水头，即无具体数据
            double rate = stationData.getWaterConsumptionLine().get(0).getValue();
            if (paramValue.getParamType() == null || paramValue.getParamValue().isNaN()) {//无约束值
                double qo = data.getInFlow();
                double qp = data.getInFlow();
                applyResult(data, qp, qo, rate, calParam, stationData);
            } else {
                ParamType type = paramValue.getParamType();
                double value = paramValue.getParamValue();
                switch (type) {
                    case Qo -> {
                        double qo = value;
                        double qp = qo;
                        applyResult(data, qp, qo, rate, calParam, stationData);
                    }
                    case Qp -> {
                        double qp = value;
                        double qo = Math.max(data.getQo(), qp);
                        applyResult(data, qp, qo, rate, calParam, stationData);
                    }
                    case dH -> {
                        double H_aft = data.getLevelBef() + value;
                        data.setLevelAft(H_aft);
                        double qo_cal = calculateQo(H_aft, data, calParam, stationData);
                        applyResult(data, qo_cal, qo_cal, rate, calParam, stationData);
                    }
                    case H -> {
                        double H_aft = value;
                        data.setLevelAft(H_aft);
                        double qo_cal = calculateQo(H_aft, data, calParam, stationData);
                        applyResult(data, qo_cal, qo_cal, rate, calParam, stationData);
                    }
                    case P -> {
                        double gen = value;
                        double qp = gen * rate / 1e-3 / calParam.getPeriod();
                        applyResult(data, qp, data.getQo(), rate, calParam, stationData);
                    }
                    default -> throw new IllegalArgumentException("不支持的参数类型: " + type);
                }
            }
        }
        return data;
    }

    /**
     * 计算出库流量
     *
     * @param H_aft
     * @param data
     * @param calParam
     * @param stationData
     * @return
     */
    private static Double calculateQo(Double H_aft, CalculateStep data, CalculateParam calParam, StationData stationData) {
        if (stationData.getReservoirStorageLine() != null && !stationData.getReservoirStorageLine().isEmpty()) {
            double dV = 1e6 * (CodeValue.linearInterpolation(H_aft, stationData.getReservoirStorageLine()) - CodeValue.linearInterpolation(data.getLevelBef(), stationData.getReservoirStorageLine()));
            return Math.max((data.getInFlow() * calParam.getPeriod() - dV) / calParam.getPeriod(), 0);
        } else {
            return data.getInFlow();
        }

    }

    /**
     * 计算末水位
     *
     * @param levelBef
     * @param qo
     * @param qin
     * @param calParam
     * @param stationData
     * @return
     */
    private static Double calculateLevelAft(Double levelBef, Double qo, Double qin, CalculateParam calParam, StationData stationData) {
        if (stationData.getReservoirStorageLine() != null && !stationData.getReservoirStorageLine().isEmpty()) {
            double dV = (qin - qo) * calParam.getPeriod() / Math.pow(10, 6);
            double V_aft = CodeValue.linearInterpolation(levelBef, stationData.getReservoirStorageLine()) + dV;
            return CodeValue.linearInterpolation(V_aft, CodeValue.exchangeCopy(stationData.getReservoirStorageLine()));
        } else {
            return levelBef;
        }
    }

    /**
     * 存储计算结果
     *
     * @param data
     * @param qp
     * @param qo
     * @param rate
     * @param calParam
     * @param stationData
     */
    private static void applyResult(CalculateStep data, double qp, double qo, double rate, CalculateParam calParam, StationData stationData) {
        data.setQp(qp);
        data.setQo(qo);
        double gen = qp * calParam.getPeriod() / rate / 1e3; // MW*h
        data.setCalGen(gen);
        double levelAft = calculateLevelAft(data.getLevelBef(), qo, data.getInFlow(), calParam, stationData);
        data.setLevelAft(levelAft);
        if (stationData.getTailLevelFlowLine() != null && !stationData.getTailLevelFlowLine().isEmpty()) {
            double headAft = data.getLevelAft() - CodeValue.linearInterpolation(qo, stationData.getTailLevelFlowLine());
            data.setHeadAft(headAft);
        }

    }

}
