package com.hust.generatingcapacity.model.generation.calculate;

import com.hust.generatingcapacity.dto.StationBaseInfDTO;
import com.hust.generatingcapacity.model.generation.domain.StationData;
import com.hust.generatingcapacity.model.generation.type.DispatchType;
import com.hust.generatingcapacity.model.generation.vo.*;
import com.hust.generatingcapacity.tools.TimeUtils;

import java.util.*;
import java.util.stream.Collectors;

public class CalDevelopmentProcess {

    /**
     * 将已有的电站运行数据和计算参数
     * 封装为CalculateVO
     * 最大和最小计算还是需要分开
     *
     * @param calculateDevelopment
     * @param stationDatas
     * @param isGenMin
     * @return
     */
    public static Map<String, CalculateVO> setCalculateVO(CalculateDevelopment calculateDevelopment, Map<String, List<StationData>> stationDatas, boolean isGenMin) {
        Map<String, CalculateVO> map = new LinkedHashMap<>();
        Date startDate = calculateDevelopment.getStartDate();
        Date endDate = calculateDevelopment.getEndDate();
        String period = calculateDevelopment.getPeriod();
        int schedulingL = TimeUtils.getDateDuration(startDate, endDate, period);
        DispatchType dispatchType = DispatchType.fromCode(calculateDevelopment.getDispatchType());
        Map<String, CalculateInput> calculateInputs = calculateDevelopment.getCalculateInputs();
        Map<String, CalculateCondition> calculateConditions = calculateDevelopment.getCalculateConditions();
        for (Map.Entry<String, List<StationData>> entry : stationDatas.entrySet()) {
            List<StationData> stationDataList = entry.getValue();
            for (StationData stationData : stationDataList) {
                String stationName = stationData.getStationName();
                //输入数据，检查入库径流长度
                CalculateInput calInput = calculateInputs.get(stationName);
                calInput.checkForecast(schedulingL);
                //计算步骤初始化
                CalculateStep calStep = new CalculateStep(calInput);
                //计算参数设置
                CalculateParam calParam = new CalculateParam();
                calParam.setStation(stationName);
                calParam.setPeriod(CalculateParam.changePeriod(period));
                calParam.setSchedulingL(schedulingL);
                Boolean isIntervalFlow = isIntervalFlow(stationName, stationDatas);
                calParam.setIntervalFlow(isIntervalFlow);
                calParam.setGenMin(isGenMin);
                boolean isConsiderH = stationData.getWaterConsumptionLine().size() > 1 && stationData.getReservoirStorageLine().size() > 1;
                calParam.setConsiderH(isConsiderH);
                calParam.setDispatchType(dispatchType);
                //封装CalculateVO
                CalculateVO calculateVO = new CalculateVO(calStep, calInput, calParam, stationData);
                //预设条件调度需要设置预设条件
                if (dispatchType.equals(DispatchType.PRE_CONDITION)) {
                    CalculateCondition calculateCondition = calculateConditions.get(stationName);
                    calculateVO.setCalCondition(calculateCondition);
                }
                map.put(stationName, calculateVO);
            }
        }
        return map;
    }

    /**
     * 判断电站是否为区间径流电站
     *
     * @param station
     * @param stationDatas
     * @return
     */
    public static Boolean isIntervalFlow(String station, Map<String, List<StationData>> stationDatas) {
        List<StationData> stationDataList = new LinkedList<>();
        boolean isIntervalFlow = false;
        for (String key : stationDatas.keySet()) {
            stationDataList.addAll(stationDatas.get(key));
        }
        for (StationData stationData : stationDataList) {
            if (stationData.getLowerStation().equals(station)) {
                isIntervalFlow = true;
                break;
            }
        }
        return isIntervalFlow;
    }

    /**
     * 输入的stations已自动更新过
     * 更新同一个流域内的电站下游关系,删除不参与计算的电站，下游电站自动更新
     *
     * @param stations
     */
    public static List<StationBaseInfDTO> updateDownRelation(List<StationBaseInfDTO> stations) {
        List<StationBaseInfDTO> calStation = stations
                .stream()
                .filter(station -> station.getIsProceduralCalculation() && station.getStatus() == 1)
                .toList();
        for (StationBaseInfDTO dto : calStation) {
            String downStationName = dto.getLowerStation();
            if (downStationName == null || downStationName.isBlank()) {
                continue;
            }
            Optional<StationBaseInfDTO> downStationOpt = calStation.stream()
                    .filter(station -> station.getStationName().equals(downStationName))
                    .findFirst();
            if (downStationOpt.isEmpty()) {//下游电站不在计算范围内,需要更新下游电站
                Optional<StationBaseInfDTO> oriDownStationOpt = stations.stream()
                        .filter(station -> station.getStationName().equals(downStationName))
                        .findFirst();
                if (oriDownStationOpt.isEmpty()) {//本流域内没有这个下游电站，证明在其它流域，不处理
                    continue;
                }
                StationBaseInfDTO oriDownStation = oriDownStationOpt.get();
                //寻找下游电站的下游电站，直到找到计算范围内的电站或者没有下游电站为止
                String nextDownStationName = oriDownStation.getLowerStation();
                while (nextDownStationName != null && !nextDownStationName.isBlank()) {
                    String finalNextDownStationName = nextDownStationName;
                    Optional<StationBaseInfDTO> nextDownStationOpt = calStation.stream()
                            .filter(station -> station.getStationName().equals(finalNextDownStationName))
                            .findFirst();
                    if (nextDownStationOpt.isPresent()) {
                        //找到了，更新当前电站的下游电站
                        dto.setLowerStation(nextDownStationName);
                        break;
                    } else {
                        //没找到，继续往下找
                        String finalNextDownStationName1 = nextDownStationName;
                        Optional<StationBaseInfDTO> nextOriDownStationOpt = stations.stream()
                                .filter(station -> station.getStationName().equals(finalNextDownStationName1))
                                .findFirst();
                        if (nextOriDownStationOpt.isEmpty()) {
                            //说明没有下游电站了，结束
                            break;
                        } else {
                            nextDownStationName = nextOriDownStationOpt.get().getLowerStation();
                        }
                    }
                }
            }
        }
        return calStation;
    }

    /**
     * 对单一流域的电站列表进行拓扑排序
     *
     * @param stations
     * @return
     */
    public static List<StationBaseInfDTO> topoSortOneBasin(List<StationBaseInfDTO> stations) {
        // 名称 -> DTO
        Map<String, StationBaseInfDTO> nameMap = new HashMap<>();
        for (StationBaseInfDTO dto : stations) {
            nameMap.put(dto.getStationName(), dto);
        }
        // 邻接表：上游 -> 下游列表
        Map<String, List<String>> graph = new HashMap<>();
        // 入度：被多少上游指向
        Map<String, Integer> inDegree = new HashMap<>();
        // 初始化入度为 0
        for (StationBaseInfDTO dto : stations) {
            inDegree.put(dto.getStationName(), 0);
            graph.putIfAbsent(dto.getStationName(), new ArrayList<>());
        }
        // 建图
        for (StationBaseInfDTO dto : stations) {
            String cur = dto.getStationName();
            // 规则1：如果有 upperStation，就认为 upper -> cur
            String upper = dto.getUpperStation();
            if (upper != null && !upper.isBlank() && nameMap.containsKey(upper)) {
                graph.get(upper).add(cur);
                inDegree.put(cur, inDegree.get(cur) + 1);
            }
            // 规则2：如果 upperStation 为空但有 lowerStation，就认为 cur -> lower
            String lower = dto.getLowerStation();
            if ((upper == null || upper.isBlank())
                    && lower != null && !lower.isBlank()
                    && nameMap.containsKey(lower)) {
                graph.get(cur).add(lower);
                inDegree.put(lower, inDegree.get(lower) + 1);
            }
        }
        // 拓扑排序（Kahn 算法）
        Queue<String> queue = new ArrayDeque<>();
        for (Map.Entry<String, Integer> e : inDegree.entrySet()) {
            if (e.getValue() == 0) {
                queue.offer(e.getKey());
            }
        }
        List<StationBaseInfDTO> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            String name = queue.poll();
            StationBaseInfDTO dto = nameMap.get(name);
            if (dto != null) {
                sorted.add(dto);
            }
            List<String> nexts = graph.getOrDefault(name, Collections.emptyList());
            for (String nxt : nexts) {
                int deg = inDegree.get(nxt) - 1;
                inDegree.put(nxt, deg);
                if (deg == 0) {
                    queue.offer(nxt);
                }
            }
        }
        // 如果有环，拓扑排序会丢一些点，这里可以兜底：把没输出到的追加在最后
        if (sorted.size() < stations.size()) {
            Set<String> already = sorted.stream()
                    .map(StationBaseInfDTO::getStationName)
                    .collect(Collectors.toSet());
            for (StationBaseInfDTO dto : stations) {
                if (!already.contains(dto.getStationName())) {
                    sorted.add(dto); // 说明有环或不完整信息，兜底放后面
                }
            }
        }
        return sorted;
    }

    /**
     * 在给定的电站列表中，找到最下游的电站名称
     *
     * @param stationBaseInfDTOS
     * @param partStationBaseInfDTOS
     * @return
     */
    public static String findMostDownstream(List<StationBaseInfDTO> stationBaseInfDTOS, List<StationBaseInfDTO> partStationBaseInfDTOS) {
        if (partStationBaseInfDTOS == null || partStationBaseInfDTOS.isEmpty()) {
            return stationBaseInfDTOS.get(stationBaseInfDTOS.size() - 1).getStationName();
        }
        // 1. 构建“电站名 → 排序位置”
        Map<String, Integer> orderMap = new HashMap<>();
        for (int i = 0; i < stationBaseInfDTOS.size(); i++) {
            orderMap.put(stationBaseInfDTOS.get(i).getStationName(), i);
        }
        // 2. 找到 part 列表中位置最大的（最下游）
        StationBaseInfDTO best = null;
        int bestIndex = -1;
        for (StationBaseInfDTO dto : partStationBaseInfDTOS) {
            Integer idx = orderMap.get(dto.getStationName());
            if (idx != null && idx > bestIndex) {
                bestIndex = idx;
                best = dto;
            }
        }
        return best != null ? best.getStationName() : partStationBaseInfDTOS.get(partStationBaseInfDTOS.size() - 1).getStationName();
    }
}
