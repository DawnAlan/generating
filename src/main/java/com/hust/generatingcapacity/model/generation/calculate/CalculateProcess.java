package com.hust.generatingcapacity.model.generation.calculate;

import com.hust.generatingcapacity.model.generation.dispatch.ConstraintEnvBuilder;
import com.hust.generatingcapacity.model.generation.vo.*;
import com.hust.generatingcapacity.model.generation.domain.ConstraintData;
import com.hust.generatingcapacity.model.generation.domain.StationData;
import com.hust.generatingcapacity.model.generation.type.ParamType;
import com.hust.generatingcapacity.tools.TimeUtils;


import java.util.*;

public class CalculateProcess {


    /**
     * 多时段多水库计算流程
     *
     * @return
     */
    public static Map<String, List<CalculateStep>> LStepAllStationCalculate(Map<String, CalculateVO> calMap) {
        Map<String, List<CalculateStep>> LStepAllStationCalculateData = new HashMap<>();
        int L = 0;
        for (String stationName : calMap.keySet()) {
            L = calMap.get(stationName).getCalInput().getL();
            LStepAllStationCalculateData.put(stationName, new ArrayList<>());
        }
        /*
        逐个电站按多时段进行计算
         */
//        for (String station : stationDataMap.keySet()) {
//            CalculateStep data = dataMap.get(station);
//            CalculateParam calParam = calParamMap.get(station);
//            CalculateInput calInput = calInputMap.get(station);
//            StationData stationData = stationDataMap.get(station);
//            if (data == null || calParam == null || calInput == null || stationData == null) {
//                throw new IllegalArgumentException("请检查电站 " + station + " 的计算数据、输入数据或电站数据是否完整！");
//            }
//            //判断是否为区间径流
//            List<CalculateStep> result;
//            if (!calParam.isIntervalFlow()) {
//                result = LStepCalculate(data, calParam, calInput, stationData);
//            } else if (stationData.getUpperStation() == null || stationData.getUpperStation().isEmpty()) {
//                result = LStepCalculate(data, calParam, calInput, stationData);
//            } else {
//                //间流计算
//                String upperStation = stationData.getUpperStation();
//                List<CalculateStep> upperDataList = LStepAllStationCalculateData.get(upperStation);
//                if (upperDataList == null || upperDataList.size() < calInput.getL()) {
//                    throw new IllegalArgumentException("请检查电站 " + station + " 的上游电站 " + upperStation + " 是否已计算！");
//                }
//                List<CalculateStep> combinedResults = new ArrayList<>();
//                for (int i = 0; i < calInput.getL(); i++) {
//                    CalculateStep upperData = upperDataList.get(i);
//                    CalculateStep currentData = data.clone();
//                    currentData.setInFlow(upperData.getQo() + currentData.getInFlow());
//                    CalculateStep stepResult = oneStepCalculate(currentData, calParam, stationData);
//                    if (stepResult != null) {
//                        combinedResults.add(stepResult); // 只取每次计算的第一个结果
//                        data = getNextStepData(stepResult, calInput); // 更新 data 以便进行下一时段的计算
//                    }
//                }
//                result = combinedResults;
//            }
//            LStepAllStationCalculateData.put(station, result);
//        }
        /*
        逐个时段按多电站计算
         */
        if (L == 0) {
            throw new IllegalArgumentException("请检查计算输入的预见期！");
        }
        for (int i = 0; i < L; i++) {
            Map<String, CalculateStep> oneStepDataMap = oneStepAllStationCalculate(calMap);
            for (String station : oneStepDataMap.keySet()) {
                List<CalculateStep> stationResults = LStepAllStationCalculateData.computeIfAbsent(station, k -> new ArrayList<>());
                stationResults.add(oneStepDataMap.get(station));
                //准备下一时段数据
                CalculateStep nextData = getNextStepData(oneStepDataMap.get(station), calMap.get(station).getCalInput());
                calMap.get(station).setCalStep(nextData);
            }
        }
        return LStepAllStationCalculateData;
    }

    /**
     * 单一时段多水库计算流程
     *
     * @return
     */
    public static Map<String, CalculateStep> oneStepAllStationCalculate(Map<String, CalculateVO> calMap) {
        Map<String, CalculateStep> oneStepAllStationCalculateData = new HashMap<>();
        String upperStation = "";
        for (String station : calMap.keySet()) {
            CalculateStep calStep = calMap.get(station).getCalStep();
            CalculateParam calParam = calMap.get(station).getCalParam();
            StationData stationData = calMap.get(station).getStationData();
            if (calStep == null || calParam == null || stationData == null) {
                throw new IllegalArgumentException("请检查电站 " + station + " 的计算数据、输入数据或电站数据是否完整！");
            }
            //判断是否为区间径流
            CalculateStep result;
            if (!calParam.isIntervalFlow()) {
                result = oneStepCalculate(calStep, calParam, stationData);
            } else if (stationData.getUpperStation() == null || stationData.getUpperStation().isEmpty()) {
                result = oneStepCalculate(calStep, calParam, stationData);
            } else {
                //间流计算
                //上游电站不计算或正在建设则使用上次计算电站作为上游电站
                if (calMap.get(stationData.getUpperStation()) != null) {
                    upperStation = stationData.getUpperStation();
                }
                CalculateStep upperData = oneStepAllStationCalculateData.get(upperStation);
                if (upperData == null) {
                    throw new IllegalArgumentException("请检查电站 " + station + " 的上游电站 " + upperStation + " 是否已计算！");
                }
                calStep.setInFlow(upperData.getQo() + calStep.getInFlow());
                result = oneStepCalculate(calStep, calParam, stationData);
            }
            oneStepAllStationCalculateData.put(station, result);
            upperStation = station;
        }
        return oneStepAllStationCalculateData;
    }

    /**
     * 多时段单一水库计算流程
     *
     * @return
     */
    public static List<CalculateStep> LStepCalculate(CalculateVO calculateVO) {
        //预见期
        Integer L = calculateVO.getCalInput().getL();
        List<CalculateStep> results = new ArrayList<>();
        for (int i = 0; i < L; i++) {
            CalculateStep stepData = oneStepCalculate(calculateVO.getCalStep(), calculateVO.getCalParam(), calculateVO.getStationData());
            //逐步输出发电能力计算结果
            System.out.println(stepData);
            results.add(stepData.clone());
            //准备下一时段数据
            CalculateStep nextData = getNextStepData(stepData, calculateVO.getCalInput());
            calculateVO.setCalStep(nextData);
        }
        return results;
    }

    /**
     * 获取下一时段的计算数据
     *
     * @param currentStep
     * @param input
     * @return
     */
    private static CalculateStep getNextStepData(CalculateStep currentStep, CalculateInput input) {
        CalculateStep nextStep = new CalculateStep();
        Date nextDate = TimeUtils.addCalendar(currentStep.getTime(), input.getPeriod(), 1);
        nextStep.setTime(nextDate);
        double nextInFlow = input.getInFlows().stream()
                .filter(p -> TimeUtils.dateCompare(nextDate, p.getTime(), input.getPeriod()))
                .findFirst()
                .map(PreFlow::getInFlow)
                .orElse(0.0);  // 如果没有符合条件的 InFlow，返回默认值 0.0
        nextStep.setInFlow(nextInFlow);
        nextStep.setRevise(false);
        nextStep.setLevelBef(currentStep.getLevelAft());
        nextStep.setHead(currentStep.getHeadAft());
        nextStep.setStation(currentStep.getStation());
        return nextStep;
    }

    /**
     * 单一时段单一水库计算流程
     *
     * @param input
     * @param calParam
     * @param stationData
     * @return
     */
    public static CalculateStep oneStepCalculate(CalculateStep input, CalculateParam calParam, StationData stationData) {
        return switch (calParam.getDispatchType()) {
            case RULE_BASED -> RuleBasedCal.run(input, calParam, stationData);
            case RULE_OPTIMIZE -> RuleOptimalCal.run(input, calParam, stationData);
            case PRE_CONDITION -> throw new IllegalArgumentException("敬请期待预设条件模型！");
        };
    }


}
