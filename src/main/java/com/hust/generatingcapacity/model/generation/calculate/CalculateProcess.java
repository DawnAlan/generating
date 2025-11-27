package com.hust.generatingcapacity.model.generation.calculate;

import com.hust.generatingcapacity.model.generation.vo.*;
import com.hust.generatingcapacity.model.generation.domain.StationData;
import com.hust.generatingcapacity.tools.TimeUtils;


import java.util.*;

public class CalculateProcess {

    /**
     * 多时段多水库计算流程
     *
     * @return
     */
    public static Map<String, List<CalculateStep>> schedulingLStepAllStationCalculate(Map<String, CalculateVO> calMap, String basin, int schedulingL) {
        Map<String, List<CalculateStep>> LStepAllStationCalculateData = new HashMap<>();
        //逐个时段按多电站计算
        for (int i = 0; i < schedulingL; i++) {
            Map<String, CalculateStep> oneStepDataMap = oneStepAllStationCalculate(calMap, basin);
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
     * 单一时段多水库计算流程(考虑多流域问题)
     * @param calMap
     * @param basin
     * @return
     */
    public static Map<String, CalculateStep> oneStepAllStationCalculate(Map<String, CalculateVO> calMap, String basin) {
        //获取目标流域内多来流电站列表
        List<String> multipleBasinStations = getMultipleBasinStations(calMap, basin);
        List<CalculateStep> multipleUpperStationCalculateData = new ArrayList<>();
        for (String multipleBasinStation : multipleBasinStations) {
            Map<String, Map<String, CalculateVO>> basinsVO = getMultipleStationUpperCalMap(calMap, multipleBasinStation);
            //逐个流域计算多来流电站上游流域电站
            for (String upperBasin : basinsVO.keySet()) {
                Map<String, CalculateVO> upperCalMap = basinsVO.get(upperBasin);
                String lastStation = upperCalMap.keySet()
                        .stream()
                        .reduce((first, second) -> second)
                        .orElse(null);
                //获取其他流域最下游电站的计算结果
                multipleUpperStationCalculateData.add(oneStepOneBasinAllStationCalculate(upperCalMap, new ArrayList<>()).get(lastStation));
            }
        }
        //进行目标流域内所有电站计算
        Map<String, CalculateVO> oneStepOneBasinAllStationCalculate = new LinkedHashMap<>();
        for (String station : calMap.keySet()) {
            StationData stationData = calMap.get(station).getStationData();
            if (stationData.getBasin().equals(basin)) {
                oneStepOneBasinAllStationCalculate.put(station, calMap.get(station));
            }
        }
        return oneStepOneBasinAllStationCalculate(oneStepOneBasinAllStationCalculate, multipleUpperStationCalculateData);

    }

    /**
     * 获取多来流电站的上游流域电站信息
     *
     * @param calMap
     * @param station
     * @return Map<String, Map<String, CalculateVO>> 第一层key是电站，第二层是流域
     */
    public static Map<String, Map<String, CalculateVO>> getMultipleStationUpperCalMap(Map<String, CalculateVO> calMap, String station) {
        Map<String, Map<String, CalculateVO>> multipleBasinCalMap = new HashMap<>();
        String basin = calMap.get(station).getStationData().getBasin();
        List<String> upperStations = new ArrayList<>();
        for (String key : calMap.keySet()) {
            StationData stationData = calMap.get(key).getStationData();
            if (stationData.getLowerStation().equals(station) && !stationData.getBasin().equals(basin)) {
                upperStations.add(stationData.getStationName());
            }
        }
        for (String upperStation : upperStations) {
            String upperBasin = calMap.get(upperStation).getStationData().getBasin();
            Map<String, CalculateVO> upperCalMap = new LinkedHashMap<>();
            for (String key : calMap.keySet()) {
                StationData stationData = calMap.get(key).getStationData();
                if (stationData.getBasin().equals(upperBasin)) {
                    upperCalMap.put(key, calMap.get(key));
                }
                if (stationData.getLowerStation().equals(upperStation)) {//只需要收集到多来流电站上游即可
                    break;
                }
            }
            multipleBasinCalMap.put(upperBasin, upperCalMap);
        }
        return multipleBasinCalMap;
    }


    /**
     * 获取目标流域中多来流的电站列表
     *
     * @param calMap
     * @param basin
     * @return
     */
    public static List<String> getMultipleBasinStations(Map<String, CalculateVO> calMap, String basin) {
        //获取目标流域内所有电站
        List<String> stations = new ArrayList<>();
        for (String station : calMap.keySet()) {
            StationData stationData = calMap.get(station).getStationData();
            if (stationData.getBasin().equals(basin)) {
                stations.add(station);
            }
        }
        //筛选出多来流电站
        int count = 0;
        List<String> stationList = new ArrayList<>();
        for (String station : stations) {
            for (String key : calMap.keySet()) {
                StationData stationData = calMap.get(key).getStationData();
                if (stationData.getLowerStation().equals(station) && !stationData.getBasin().equals(basin)) {
                    count++;
                }
            }
            if (count >= 1) {
                stationList.add(station);
            }
            count = 0;
        }
        return stationList;
    }

    /**
     * 单一时段多水库计算流程
     *
     * @return
     */
    public static Map<String, CalculateStep> oneStepOneBasinAllStationCalculate(Map<String, CalculateVO> calMap, List<CalculateStep> otherBasinCalSteps) {
        Map<String, CalculateStep> oneStepAllStationCalculateData = new HashMap<>();
        //上游电站计算结果暂存
        List<String> upperStations = new ArrayList<>();
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
            if (calParam.isIntervalFlow()) {
                double outFlowInBasin = 0.0;
                //寻找流域内上游电站计算结果
                for (String key : calMap.keySet()) {
                    if (calMap.get(key).getStationData().getLowerStation().equals(station)) {
                        upperStations.add(key);
                    }
                }
                //如果上游没有电站则采用上一级计算结果
                if (upperStations.isEmpty()) {
                    CalculateStep upperData = oneStepAllStationCalculateData.get(upperStation);
                    if (upperData == null) {
                        upperData = new CalculateStep();
                    }
                    outFlowInBasin += upperData.getQo();
                } else {
                    for (String upper : upperStations) {
                        CalculateStep upperData = oneStepAllStationCalculateData.get(upper);
                        if (upperData == null) {
                            upperData = new CalculateStep();
                        }
                        outFlowInBasin += upperData.getQo();
                    }
                }
                //合并其他流域上游电站出库流量作为入库流量
                double outFlowOutBasin = 0.0;
                if (otherBasinCalSteps != null && !otherBasinCalSteps.isEmpty()) {
                    for (CalculateStep otherBasinCalStep : otherBasinCalSteps) {
                        outFlowOutBasin += otherBasinCalStep.getQo();
                    }
                }
                calStep.setInFlow(outFlowInBasin + calStep.getInFlow() + outFlowOutBasin);
            }
            result = oneStepCalculate(new CalculateVO(calStep, calParam, stationData));
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
        Integer L = calculateVO.getCalParam().getSchedulingL();
        List<CalculateStep> results = new ArrayList<>();
        for (int i = 0; i < L; i++) {
            CalculateStep stepData = oneStepCalculate(calculateVO);
            //逐步输出发电能力计算结果
            System.out.println(stepData.toString(i));
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
     * @return
     */
    public static CalculateStep oneStepCalculate(CalculateVO calculateVO) {
        CalculateStep result = new CalculateStep();
        switch (calculateVO.getCalParam().getDispatchType()) {
            case RULE_BASED:
                result = RuleBasedCal.run(calculateVO);
                break;
            case RULE_OPTIMIZE:
                result = RuleOptimalCal.run(calculateVO);
                break;
            case PRE_CONDITION:
                result = PreConditionCal.run(calculateVO);
                break;
        }
        System.out.println(calculateVO.getStationData().getStationName() + "电站发电能力结果为：\n" + result.toString());
        return result;
    }


}
