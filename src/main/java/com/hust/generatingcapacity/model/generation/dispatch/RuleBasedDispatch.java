package com.hust.generatingcapacity.model.generation.dispatch;

import com.hust.generatingcapacity.model.generation.domain.*;
import com.hust.generatingcapacity.model.generation.type.ParamType;
import com.hust.generatingcapacity.model.generation.vo.CalculateParam;
import com.hust.generatingcapacity.model.generation.vo.CalculateStep;
import com.hust.generatingcapacity.model.generation.vo.ParamValue;

public class RuleBasedDispatch {

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
            rate = CodeValue.linearInterpolation(data.getLevelBef(), stationData.getWaterConsumptionLine());
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
            double rate = CodeValue.linearInterpolation(data.getLevelBef(), stationData.getWaterConsumptionLine());
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
            double rate = CodeValue.linearInterpolation(data.getLevelBef(), stationData.getWaterConsumptionLine());
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
