package com.hust.generatingcapacity.model.generation.calculate;

import com.hust.generatingcapacity.GeneratingCapacityApplication;
import com.hust.generatingcapacity.dto.StationBaseInfDTO;
import com.hust.generatingcapacity.iservice.IHydropowerStationService;
import com.hust.generatingcapacity.model.generation.domain.StationData;
import com.hust.generatingcapacity.model.generation.type.PreConditionType;
import com.hust.generatingcapacity.model.generation.vo.*;
import com.hust.generatingcapacity.repository.HydropowerStationRepository;
import com.hust.generatingcapacity.tools.ExcelUtils;
import com.hust.generatingcapacity.tools.TimeUtils;
import com.hust.generatingcapacity.tools.Tools;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.text.SimpleDateFormat;
import java.util.*;

@SpringBootTest(classes = GeneratingCapacityApplication.class)
public class BasinCalculateTest {
    @Autowired
    private IHydropowerStationService hydropowerStationService;
    @Autowired
    private HydropowerStationRepository hydropowerStationRepository;

    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    @Test
    void basinRuleBasedTest() {
        String basin = "大渡河";
        String dispatchType = "规程优化";
        Date start = sdf.parse("2023-01-01", new java.text.ParsePosition(0));
        Date end = sdf.parse("2023-01-03", new java.text.ParsePosition(0));
        int schedulingL = 1;
        String period = "日";
        boolean isGenMin = true; //是否计算最小发电能力
        //获取各流域电站数据
        Map<String, List<StationData>> basinStationDataMap = getStationDataMap(basin);
        Map<String, StationData> stationDataMap = new LinkedHashMap<>();
        basinStationDataMap.forEach((basinName, stationDataList) -> {
            stationDataList.forEach(stationData -> {
                stationDataMap.put(stationData.getStationName(), stationData);
            });
        });
        Map<String, Object[][]> dataMap = new HashMap<>();
        for (String stationName : stationDataMap.keySet()) {
            String basinName = stationDataMap.get(stationName).getBasin();
            Object[][] data = new Object[0][0];
            try {
                data = ExcelUtils.readExcel("D:\\Data\\5.大渡河\\整理数据\\大渡河流域内部发电能力预测\\发电计算\\全流域计算\\" + basinName + "\\" + basinName + "水电站2023年日尺度整合数据.xlsx", stationName);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            dataMap.put(stationName, data);
        }
        Map<String, List<CalculateStep>> result = new HashMap<>();
        //计算单一方案
        int number = TimeUtils.getDateDuration(start, end, period);
        for (int genMin = 0; genMin <= 1; genMin++) {
            if (genMin > 0) {
                isGenMin = true;
            }
            for (int i = 0; i < number; i++) {//计算多个方案
                Date startDate = TimeUtils.addCalendar(start, period, i);
                Date endDate = TimeUtils.addCalendar(start, period, i + schedulingL);
                CalculateDevelopment calculateDevelopment = new CalculateDevelopment();
                //获取各电站输入信息
                Map<String, CalculateInput> calculateInputs = new LinkedHashMap<>();
                Map<String, CalculateCondition> calculateConditions = new LinkedHashMap<>();
                for (String stationName : stationDataMap.keySet()) {
                    CalculateInput calculateInput = new CalculateInput(stationName, startDate, period);
                    getCalInputFromExcel(dataMap.get(stationName), calculateInput);
                    if (calculateInput.getInFlows().isEmpty()) {
                        List<PreFlow> inFlows = new ArrayList<>();
                        for (int j = 0; j < schedulingL; j++) {
                            Date flowDate = TimeUtils.addCalendar(startDate, period, j);
                            inFlows.add(new PreFlow(flowDate, 0.0));
                        }
                        calculateInput.setInFlows(inFlows);
                        System.out.println("注意！电站" + stationName + "无入库流量数据，默认入库为0。");
                    }
                    calculateInput.checkForecast(schedulingL);
                    calculateInputs.put(stationName, calculateInput);
                    //获取电站约束（待完善）预设条件模型目前按照末水位来计算
                    if (dispatchType.equals(com.hust.generatingcapacity.model.generation.type.DispatchType.PRE_CONDITION.getDesc())) {
                        CalculateCondition calculateCondition = new CalculateCondition();
                        calculateCondition.setStation(stationName);
                        calculateCondition.setPreCondition(PreConditionType.H_after);
                        double hAfter = getHAfterFromExcel(dataMap.get(stationName), TimeUtils.addCalendar(startDate, period, 1), period);
                        if (hAfter == 0.0) {
                            hAfter = calculateInput.getWaterLevel(); //防守
                        }
                        calculateCondition.setPreValue(hAfter);
                        calculateConditions.put(stationName, calculateCondition);
                    }
                }
                //组装计算对象
                calculateDevelopment.setBasin(basin);
                calculateDevelopment.setStartDate(startDate);
                calculateDevelopment.setEndDate(endDate);
                calculateDevelopment.setDispatchType(dispatchType);
                calculateDevelopment.setPeriod(period);
                calculateDevelopment.setCalculateInputs(calculateInputs);
                calculateDevelopment.setCalculateConditions(calculateConditions); //预设条件的时候在赋值
                //输入数据封装
                Map<String, CalculateVO> calculateVO = CalDevelopmentProcess.setCalculateVO(calculateDevelopment, basinStationDataMap, isGenMin);
                // 当前时段所有电站的计算结果
                Map<String, List<CalculateStep>> current = CalculateProcess.schedulingLStepAllStationCalculate(calculateVO, basin, schedulingL);
                // 累积到 result
                for (Map.Entry<String, List<CalculateStep>> entry : current.entrySet()) {
                    String station = entry.getKey();
                    List<CalculateStep> steps = entry.getValue();
                    result.computeIfAbsent(station, k -> new ArrayList<>()).addAll(steps);
                    System.out.println(station + " 第" + i + " 时段计算完成………………");
                }
            }
            //记录至表格
            for (String station : stationDataMap.keySet()) {
                List<CalculateStep> steps = result.get(station);
                if (steps == null || steps.isEmpty()) {
                    break;
                }
                Object[][] res = new Object[steps.size() + 1][];
                res[0] = new Object[]{"时间", "是否修正", "入库径流", "开始水位", "结束水位", "发电流量", "出库流量", "规程计算结果", "历史真实发电（MW*H）", "警告信息"};
                for (int i = 0; i < steps.size(); i++) {
                    int finalI = i;
                    Object value = Arrays.stream(dataMap.get(station))
                            .skip(1)
                            .filter(d -> TimeUtils.dateCompare((Date) ((Object[]) d)[0], steps.get(finalI).getTime(), period))
                            .findFirst()   // 返回 Optional<Object>
                            .map(d -> ((Object[]) d)[5])  // 取下标
                            .orElse(null); // 如果没找到，返回 null
                    res[i + 1] = new Object[]{
                            steps.get(i).getTime(), steps.get(i).isRevise(), steps.get(i).getInFlow(),
                            steps.get(i).getLevelBef(), steps.get(i).getLevelAft(),
                            steps.get(i).getQp(), steps.get(i).getQo(),
                            steps.get(i).getCalGen(), value, steps.get(i).getRemark()
                    };
                }
                if (!isGenMin) {
                    ExcelUtils.writeExcel("D:\\Data\\5.大渡河\\整理数据\\大渡河流域内部发电能力预测\\发电计算\\全流域计算\\" + basin + "\\" + dispatchType + "\\2023年日尺度最大发电能力计算结果" + "-预见期" + schedulingL + "天.xlsx", station, res);
                } else {
                    ExcelUtils.writeExcel("D:\\Data\\5.大渡河\\整理数据\\大渡河流域内部发电能力预测\\发电计算\\全流域计算\\" + basin + "\\" + dispatchType + "\\2023年日尺度最小发电能力计算结果" + "-预见期" + schedulingL + "天.xlsx", station, res);
                }
            }
            genMin++;
        }

    }

    private static double getHAfterFromExcel(Object[][] data, Date date, String period) {
        if (data.length == 0) {
            // 完全找不到有水位的记录，这里你要自己定策略：用默认值 / 抛异常 / 记录 remark
            return 0;
        }
        // 先建 filterData
        Object[][] filterData = Arrays.stream(data)
                .skip(1)
                .filter(d -> TimeUtils.isAfterOrSame((Date) d[0], date, period))
                .toArray(Object[][]::new);
        // 先在窗口内找
        Optional<Object[]> optRow = Arrays.stream(filterData)
                .filter(r -> !(r[1] == null && r[2] == null && r[3] == null))
                .findFirst();
        Object[] rowForLevel;
        // 窗口内没有，就在全体数据里找最近的
        rowForLevel = optRow.orElseGet(() -> Arrays.stream(data)
                .skip(1)
                .filter(r -> !(r[1] == null && r[2] == null && r[3] == null))
                .min(Comparator.comparingLong(r ->
                        Math.abs(((Date) r[0]).getTime() - date.getTime())
                ))
                .orElse(null));
        if (rowForLevel != null) {
            return Tools.changeObjToDouble(rowForLevel[3]);
        } else {
            // 完全找不到有水位的记录，这里你要自己定策略：用默认值 / 抛异常 / 记录 remark
            return 0;
        }
    }

    private static void getCalInputFromExcel(Object[][] data, CalculateInput input) {
        if (data.length == 0) {
            // 完全找不到有水位的记录，这里你要自己定策略：用默认值 / 抛异常 / 记录 remark
            input.setWaterLevel(0.0);
            input.setTailLevel(-10086.0);
            input.setInFlows(new ArrayList<>());
        }
        // 先建 filterData
        Object[][] filterData = Arrays.stream(data)
                .skip(1)
                .filter(d -> TimeUtils.isAfterOrSame((Date) d[0], input.getStart(), input.getPeriod()))
                .toArray(Object[][]::new);
        // 先在窗口内找
        Optional<Object[]> optRow = Arrays.stream(filterData)
                .filter(r -> !(r[1] == null && r[2] == null && r[3] == null))
                .findFirst();
        Object[] rowForLevel;
        if (optRow.isPresent()) {
            rowForLevel = optRow.get();
        } else {
            // 窗口内没有，就在全体数据里找最近的
            Date target = input.getStart();
            rowForLevel = Arrays.stream(data)
                    .skip(1)
                    .filter(r -> !(r[1] == null && r[2] == null && r[3] == null))
                    .min(Comparator.comparingLong(r ->
                            Math.abs(((Date) r[0]).getTime() - target.getTime())
                    ))
                    .orElse(null);
        }
        if (rowForLevel != null) {
            input.setWaterLevel(Tools.changeObjToDouble(rowForLevel[3]));
            input.setTailLevel(Tools.changeObjToDouble(rowForLevel[4]));
        } else {
            // 完全找不到有水位的记录，这里你要自己定策略：用默认值 / 抛异常 / 记录 remark
            input.setWaterLevel(0.0);
            input.setTailLevel(-10086.0);
        }
        // 流量建议过滤一下 null
        input.setInFlows(
                Arrays.stream(filterData)
                        .filter(d -> d.length > 1 && d[1] != null)
                        .map(d -> new PreFlow((Date) d[0], Tools.changeObjToDouble(d[1])))
                        .toList()
        );
    }

    /**
     * 获取某流域下的所有需要计算的电站数据，包含其他流域对该流域有补充的电站
     *
     * @param basin
     * @return
     */
    public Map<String, List<StationData>> getStationDataMap(String basin) {
        List<String> stations = hydropowerStationRepository.findAllStationByBasin(basin);
        Map<String, List<StationBaseInfDTO>> calBasinStations = new LinkedHashMap<>();
        List<String> upperStations = new ArrayList<>();
        //首先收集涉及到其他流域中的上游电站
        for (String station : stations) {
            upperStations.addAll(hydropowerStationRepository.findUpperStationByLowerStationDifferentBasin(station, basin));
        }
        //然后按流域进行划分(key为basin)
        Map<String, List<StationBaseInfDTO>> otherBasinUpperStations = new HashMap<>();
        for (String calStation : upperStations) {
            StationBaseInfDTO stationBaseInfDTO = hydropowerStationRepository.findBaseByStationName(calStation);
            String basinName = stationBaseInfDTO.getBasin();
            if (!otherBasinUpperStations.containsKey(basinName)) {
                otherBasinUpperStations.put(basinName, new ArrayList<>(Collections.singleton(stationBaseInfDTO)));
            } else {
                otherBasinUpperStations.get(basinName).add(stationBaseInfDTO);
            }
        }
        //然后依次获取各流域的部分电站
        for (String otherBasin : otherBasinUpperStations.keySet()) {
            //流域内电站的拓扑排序
            List<StationBaseInfDTO> stationBaseInfDTOS = hydropowerStationService.getBasinStations(otherBasin);
            stationBaseInfDTOS = CalDevelopmentProcess.topoSortOneBasin(stationBaseInfDTOS);
            //获取该流域对其他流域径流补充的最后一个电站
            List<StationBaseInfDTO> partStationBaseInfDTOS = otherBasinUpperStations.get(otherBasin);
            String lastStation = CalDevelopmentProcess.findMostDownstream(stationBaseInfDTOS, partStationBaseInfDTOS);
            //在这个电站之前的都是需要计算的
            List<StationBaseInfDTO> needCalStations = new ArrayList<>();
            for (StationBaseInfDTO stationBaseInfDTO : stationBaseInfDTOS) {
                //只计算程序化计算且投产的电站
                if (!stationBaseInfDTO.getIsProceduralCalculation() || stationBaseInfDTO.getStatus() != 1) {
                    continue;
                }
                needCalStations.add(stationBaseInfDTO);
                if (stationBaseInfDTO.getStationName().equals(lastStation)) {
                    break;
                }
            }
            calBasinStations.put(otherBasin, needCalStations);
        }
        //本流域内需要计算的电站
        List<StationBaseInfDTO> stationList = hydropowerStationRepository.findAllBaseByStationName(basin);
        calBasinStations.put(basin, stationList);
        //然后转换为Map<String,List<StationData>>
        Map<String, List<StationData>> stationDataMap = new LinkedHashMap<>();
        for (String calBasin : calBasinStations.keySet()) {
            List<StationData> stationDataList = new ArrayList<>();
            List<StationBaseInfDTO> stationBaseInfDTOS = calBasinStations.get(calBasin);
            //排序
            List<StationBaseInfDTO> sortedStations = CalDevelopmentProcess.topoSortOneBasin(stationBaseInfDTOS);
            //筛选计算电站并更新下游关系
            stationList = CalDevelopmentProcess.updateDownRelation(stationList);
            for (StationBaseInfDTO dto : stationList) {
                StationData stationData = hydropowerStationService.changeToStationData(hydropowerStationService.get(dto.getStationName()));
                stationData.setLowerStation(dto.getLowerStation());
                stationDataList.add(stationData);
            }
            stationDataMap.put(calBasin, stationDataList);
        }
        return stationDataMap;
    }

}
