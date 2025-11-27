package com.hust.generatingcapacity.model.generation.calculate;

import com.hust.generatingcapacity.model.generation.domain.CodeValue;
import com.hust.generatingcapacity.model.generation.domain.NHQData;
import com.hust.generatingcapacity.model.generation.domain.StationData;
import com.hust.generatingcapacity.model.generation.type.PreConditionType;
import com.hust.generatingcapacity.model.generation.vo.*;
import com.hust.generatingcapacity.tools.TimeUtils;

import java.util.List;

public class PreConditionCal {

    public static CalculateStep run(CalculateVO calculateVO) {
        PreConditionType preConditionType = calculateVO.getCalCondition().getPreCondition();
        return switch (preConditionType) {
            case H_after -> calHAfter(calculateVO);
            case Q_power -> calQPower(calculateVO);
            default -> null;
        };
    }

    public static CalculateStep calQPower(CalculateVO calculateVO) {
        //信息提取
        CalculateStep data = calculateVO.getCalStep();
        CalculateParam calParam = calculateVO.getCalParam();
        CalculateInput input = calculateVO.getCalInput();
        CalculateCondition calCondition = calculateVO.getCalCondition();
        StationData stationData = calculateVO.getStationData();
        //特征曲线
        List<CodeValue> reservoirStorageLine = stationData.getReservoirStorageLine();
        List<CodeValue> waterConsumptionLine = stationData.getWaterConsumptionLine();
        //计算过程
        double Qo = calCondition.getPreValue();
        double storageBef = CodeValue.linearInterpolation(data.getLevelBef(), reservoirStorageLine);
        double dV = (data.getInFlow() - Qo) * calParam.getPeriod();
        double storageAfter = storageBef + dV / 1e6;
        double levelAfter = CodeValue.codeLinearInterpolation(storageAfter, reservoirStorageLine);
        double qpMax;
        double Lc = (CodeValue.linearInterpolation(levelAfter, waterConsumptionLine) + CodeValue.linearInterpolation(data.getLevelBef(), waterConsumptionLine)) / 2;
        if (!stationData.getNHQLines().isEmpty() && input.getTailLevel() < 0) {//有NHQ且有尾水位
            qpMax = stationData.getNHQLines().stream()
                    .mapToDouble(line -> NHQData.getMaxQ(data.getHead(), line))
                    .sum();
        } else {
            qpMax = stationData.getInstalledCapacity() / 3.6 * Lc;
        }
        double Qp = Math.min(calCondition.getPreValue(), qpMax);
        double gen = Qp * calParam.getPeriod() / Lc / 1e3;//(MW*H)
        //预设出库流量计算
        data.setQp(Qp);
        data.setQo(Qo);
        data.setCalGen(gen);
        data.setLevelAft(levelAfter);
        return data;
    }

    /**
     * 预设末水位计算
     *
     * @param calculateVO
     * @return
     */
    public static CalculateStep calHAfter(CalculateVO calculateVO) {
        //信息提取
        CalculateStep data = calculateVO.getCalStep();
        CalculateParam calParam = calculateVO.getCalParam();
        CalculateInput input = calculateVO.getCalInput();
        CalculateCondition calCondition = calculateVO.getCalCondition();
        StationData stationData = calculateVO.getStationData();
        //特征曲线
        List<CodeValue> reservoirStorageLine = stationData.getReservoirStorageLine();
        List<CodeValue> waterConsumptionLine = stationData.getWaterConsumptionLine();
        //计算过程
        double dH = calCondition.getPreValue() - data.getLevelBef();
        int dL = TimeUtils.getDateDuration(input.getStart(), data.getTime(), input.getPeriod());
        dH = dH * dL / calParam.getSchedulingL();
        double levelAfter = data.getLevelBef() + dH;
        double storageBef = CodeValue.linearInterpolation(data.getLevelBef(), reservoirStorageLine);
        double storageAfter = CodeValue.linearInterpolation(levelAfter, reservoirStorageLine);
        double dV = (storageAfter - storageBef) * 1e6;
        double Qin = data.getInFlow();
        double Qo = Qin - dV / calParam.getPeriod();
        double qpMax;
        double Lc = (CodeValue.linearInterpolation(levelAfter, waterConsumptionLine) + CodeValue.linearInterpolation(data.getLevelBef(), waterConsumptionLine)) / 2;
        if (!stationData.getNHQLines().isEmpty() && input.getTailLevel() < 0) {//有NHQ且有尾水位
            qpMax = stationData.getNHQLines().stream()
                    .mapToDouble(line -> NHQData.getMaxQ(data.getHead(), line))
                    .sum();
        } else {
            qpMax = stationData.getInstalledCapacity() / 3.6 * Lc;
        }
        double Qp = Math.min(Qo, qpMax);
        double gen = Qp * calParam.getPeriod() / Lc / 1e3;//(MW*H)
        //预设末水位计算
        data.setQp(Qp);
        data.setQo(Qo);
        data.setCalGen(gen);
        data.setLevelAft(levelAfter);
        return data;
    }


}
