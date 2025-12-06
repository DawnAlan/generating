package com.hust.generatingcapacity.model.generation.calculate;

import com.hust.generatingcapacity.model.generation.domain.ConstraintData;
import com.hust.generatingcapacity.model.generation.type.DispatchType;
import com.hust.generatingcapacity.model.generation.type.ParamType;
import com.hust.generatingcapacity.model.generation.type.PreConditionType;
import com.hust.generatingcapacity.model.generation.util.DisplayUtils;
import com.hust.generatingcapacity.model.generation.vo.*;
import com.hust.generatingcapacity.model.generation.domain.StationData;
import com.hust.generatingcapacity.tools.TimeUtils;


import java.util.*;

public class CalculateProcess {

    /**
     * 多时段多水库计算流程(考虑多流域问题)
     *
     * @param calMap      key为电站名称，value为电站计算对象
     * @param basin
     * @param schedulingL
     * @return
     */
    public static Map<String, List<CalculateStep>> schedulingLStepAllStationCalculate(Map<String, CalculateVO> calMap, String basin, int schedulingL) {
        Map<String, List<CalculateStep>> LStepAllStationCalculateData = new HashMap<>();
        //逐个时段按多电站计算
        for (int i = 0; i < schedulingL; i++) {
            //单一时段多水库计算
            Map<String, CalculateStep> oneStepDataMap = oneStepAllStationCalculate(calMap, basin);
            //分配输电断面容量
            oneStepDataMap = distributeTransmissionCapacity(oneStepDataMap, calMap, basin);
            //逐步更新发电能力计算结果
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
     * 分配输电断面容量
     *
     * @param basinStationDatas
     */
    private static Map<String, CalculateStep> distributeTransmissionCapacity(Map<String, CalculateStep> oneStepDataMap, Map<String, CalculateVO> basinStationDatas, String basin) {
        List<StationData> stationDataList = new ArrayList<>();
        int period = 0;
        boolean needDistribute = false;
        for (String stationName : basinStationDatas.keySet()) {
            stationDataList.add(basinStationDatas.get(stationName).getStationData());
            period = basinStationDatas.get(stationName).getCalParam().getPeriod();
        }
        //对于流域进行输电断面的审查
        List<String> transmissionSections = stationDataList.stream()
                .filter(s -> s.getConstraints().stream()
                        .anyMatch(c -> c.getConstraintType().equals("通道约束")))
                .map(StationData::getTransmissionSection)
                .distinct()
                .filter(Objects::nonNull)
                .filter(s -> !s.isEmpty())
                .toList();
        for (String section : transmissionSections) {
            List<StationData> stationsInSection = stationDataList.stream()
                    .filter(stationData -> section.equals(stationData.getTransmissionSection()))
                    .toList();
            stationsInSection = stationsInSection.stream().filter(s -> s.getConstraints().stream()
                            .anyMatch(c -> c.getConstraintType().equals("通道约束")))
                    .toList();
            double totalCapacity = stationsInSection.stream()
                    .mapToDouble(StationData::getInstalledCapacity)
                    .sum();
            double totalGenCapacity = stationsInSection.stream()
                    .mapToDouble(s -> oneStepDataMap.get(s.getStationName()).getCalGen())
                    .sum() / ((double) period / 3600);
            // 获取该输电断面的约束信息
            List<ConstraintData> constraints = stationsInSection.get(0).getConstraints();
            String param = constraints.stream()
                    // 先把每个约束里的 param 列表展开成一个大的 Stream<String>
                    .flatMap(c -> c.getParam().stream())
                    // 然后过滤出 param == ParamType.C 的表达式
                    .filter(exp -> {
                        String name = DisplayUtils.getMessageFromExp(exp, "param");
                        return ParamType.C.name().equals(name);
                    })
                    // 拿第一个匹配的
                    .findFirst()
                    .orElse(null);
            double transmissionCapacity = DisplayUtils.getMessageFromExp(param, "value") != null ?
                    Double.parseDouble(DisplayUtils.getMessageFromExp(param, "value")) : Double.MAX_VALUE;
            //这里可以设置每个电站在该输电断面的分配容量
            if (totalGenCapacity > transmissionCapacity) {
                needDistribute = true;
                System.out.println("流域 " + basin + " 中输电断面 " + section + " 发电能力总和 " + totalGenCapacity + " MW 超过输电断面容量 " + transmissionCapacity + " MW，按装机容量比例分配输电断面容量。");
                for (StationData stationData : stationsInSection) {
                    stationData.setTransmissionCapacity(stationData.getInstalledCapacity() / totalCapacity * transmissionCapacity);
                }
            }
        }
        if (needDistribute) {//需要进行输电断面容量分配后重新计算
            return oneStepAllStationCalculate(basinStationDatas, basin);
        } else {
            return oneStepDataMap;
        }

    }

    /**
     * 单一时段多水库计算流程(考虑多流域问题)
     *
     * @param calMap key为电站名称，value为电站计算对象
     * @param basin  目标流域名称
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
        Map<String, CalculateVO> oneStepOneBasinCalculateVO = new LinkedHashMap<>();
        for (String station : calMap.keySet()) {
            StationData stationData = calMap.get(station).getStationData();
            if (stationData.getBasin().equals(basin)) {
                oneStepOneBasinCalculateVO.put(station, calMap.get(station));
            }
        }
        return oneStepOneBasinAllStationCalculate(oneStepOneBasinCalculateVO, multipleUpperStationCalculateData);
    }


    /**
     * 单一时段多水库计算流程
     *
     * @return
     */
    public static Map<String, CalculateStep> oneStepOneBasinAllStationCalculate(Map<String, CalculateVO> calMap, List<CalculateStep> otherBasinCalSteps) {
        Map<String, CalculateStep> oneStepAllStationCalculateData = new LinkedHashMap<>();
        //上游电站计算结果暂存
        String upperStation = "";
        for (String station : calMap.keySet()) {
            CalculateStep calStep = calMap.get(station).getCalStep().clone();
            CalculateParam calParam = calMap.get(station).getCalParam();
            StationData stationData = calMap.get(station).getStationData();
            if (calStep == null || calParam == null || stationData == null) {
                throw new IllegalArgumentException("请检查电站 " + station + " 的计算数据、输入数据或电站数据是否完整！");
            }
            //判断是否为区间径流
            CalculateStep result;
            if (calParam.isIntervalFlow()) {
                double outFlowInBasin = 0.0;
                List<String> upperStations = new ArrayList<>();
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
            if (calParam.getDispatchType().equals(DispatchType.PRE_CONDITION)) {
                result = oneStepCalculate(new CalculateVO(calMap.get(station).getCalInput(), calStep, calParam, calMap.get(station).getCalCondition(), stationData));
            } else {
                result = oneStepCalculate(new CalculateVO(calStep, calParam, stationData));
            }
            oneStepAllStationCalculateData.put(station, result);
            upperStation = station;
        }
        return oneStepAllStationCalculateData;
    }

    /**
     * 单一时段单一水库计算流程
     *
     * @return
     */
    public static CalculateStep oneStepCalculate(CalculateVO calculateVO) {
        CalculateStep result;
        //无调度规程则直接预设条件，当作无调节电站计算
        if (calculateVO.getStationData().getConstraints().isEmpty()) {
            System.out.println("电站 " + calculateVO.getCalParam().getStation() + " 无调度规程，采用预设条件模型计算发电能力。");
            CalculateCondition calCondition = new CalculateCondition(PreConditionType.Q_power, calculateVO.getStationData().getStationName());
            calculateVO.setCalCondition(calCondition);
            result = new PreConditionCal().run(calculateVO);
        } else if (calculateVO.getStationData().getReservoirStorageLine().isEmpty()) {//无库容曲线则采用规程边界模型计算
            System.out.println("电站 " + calculateVO.getCalParam().getStation() + " 无库容曲线，采用规程边界模型计算发电能力。");
            result = switch (calculateVO.getCalParam().getDispatchType()) {
                case RULE_BASED, RULE_OPTIMIZE -> new RuleBasedCal().run(calculateVO);
                case PRE_CONDITION -> new PreConditionCal().run(calculateVO);
            };
        } else {
            result = switch (calculateVO.getCalParam().getDispatchType()) {
                case RULE_BASED -> new RuleBasedCal().run(calculateVO);
                case RULE_OPTIMIZE -> new RuleOptimalCal().run(calculateVO);
                case PRE_CONDITION -> new PreConditionCal().run(calculateVO);
            };
        }
        System.out.println(calculateVO.getStationData().getStationName() + "电站发电能力结果为：\n" + result.toString());
        return result;
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
                if (stationData.getLowerStation() != null && stationData.getLowerStation().equals(station) && !stationData.getBasin().equals(basin)) {
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


}
