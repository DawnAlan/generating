package com.hust.generatingcapacity.model.generation.calculate;

import com.hust.generatingcapacity.GeneratingCapacityApplication;
import com.hust.generatingcapacity.dto.StationBaseInfDTO;
import com.hust.generatingcapacity.iservice.IHydropowerStationService;
import com.hust.generatingcapacity.model.generation.domain.StationData;
import com.hust.generatingcapacity.model.generation.vo.CalculateDevelopment;
import com.hust.generatingcapacity.model.generation.vo.CalculateInput;
import com.hust.generatingcapacity.model.generation.vo.PreFlow;
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
public class CalDevelopmentProcessTest {
    @Autowired
    private IHydropowerStationService hydropowerStationService;
    @Autowired
    private HydropowerStationRepository hydropowerStationRepository;

    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    @Test
    void testSimplex() {
        String basin = "大渡河";
        Date startDate = new Date();
        Date endDate = new Date();
        String DispatchType = "规则调度";
        String period = "日";
        CalculateDevelopment calculateDevelopment = new CalculateDevelopment();
        //获取各流域电站数据
        Map<String, List<StationData>> basinStationDataMap = getStationDataMap(basin);
        Map<String,StationData> stationDataMap = new LinkedHashMap<>();
        basinStationDataMap.forEach((basinName, stationDataList) -> {stationDataList.forEach(stationData -> {stationDataMap.put(basinName, stationData);});});
        //获取各电站输入信息
        Map<String, CalculateInput> calculateInputs = new LinkedHashMap<>();
        Map<String, Object[][]> dataMap = new HashMap<>();
        for (String stationName : stationDataMap.keySet()) {
            Object[][] data = ExcelUtils.readExcel("D:\\Data\\5.大渡河\\整理数据\\大渡河流域内部发电能力预测\\发电计算\\枯期测试-大渡河\\大渡河水电站20年枯期日尺度整合数据.xlsx", stationName);
            dataMap.put(stationName, data);
        }
        for (String stationName : stationDataMap.keySet()) {
            CalculateInput calculateInput = new CalculateInput(stationName,startDate,period);
            calculateInput.checkForecast(TimeUtils.getDateDuration(startDate,endDate,period));
            calculateInputs.put(stationName,calculateInput);
        }
        //组装计算对象
        calculateDevelopment.setBasin(basin);
        calculateDevelopment.setStartDate(startDate);
        calculateDevelopment.setEndDate(endDate);
        calculateDevelopment.setDispatchType(DispatchType);
        calculateDevelopment.setPeriod(period);
        calculateDevelopment.setCalculateInputs(calculateInputs);
        calculateDevelopment.setCalculateConditions(null); //预设条件的时候在赋值
        System.out.println("done");
        //完成各流域电站的收集和排序,进行后续输入数据处理
    }

    private static CalculateInput setCalculateInput(String stationName, Date date, Object[][] data, int L, String period) {
        CalculateInput input = new CalculateInput();
        input.setStation(stationName);
        input.setStart(date);
        input.setPeriod(period);
        getCalInputFromExcel(data, input,L);
        input.checkForecast(L);
        return input;
    }

    private static void getCalInputFromExcel(Object[][] data, CalculateInput input,int L) {
        Object[][] filterData = Arrays.stream(data)
                .skip(1)
                .filter(d -> TimeUtils.isAfterOrSame((Date) d[0], input.getStart(), input.getPeriod()))
                .limit(L)
                .toArray(Object[][]::new);
        input.setWaterLevel(Tools.changeObjToDouble(filterData[0][3]));
        input.setTailLevel(Tools.changeObjToDouble(filterData[0][4]));
        input.setInFlows(Arrays.stream(filterData).map(d -> new PreFlow((Date) d[0], Tools.changeObjToDouble(d[1]))).toList());
    }

    /**
     * 获取某流域下的所有需要计算的电站数据，包含其他流域对该流域有补充的电站
     * @param basin
     * @return Map<String, List<StationData>>  key为流域名称，value为该流域下需要计算的电站列表
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

        //然后将所以涉及到的流域进行转换Map<String,List<StationData>>
        Map<String, List<StationData>> stationDataMap = new LinkedHashMap<>();
        for (String calBasin : calBasinStations.keySet()) {
            List<StationData> stationDataList = new ArrayList<>();
            List<StationBaseInfDTO> stationBaseInfDTOS = calBasinStations.get(calBasin);
            //排序
            List<StationBaseInfDTO> sortedStations = CalDevelopmentProcess.topoSortOneBasin(stationBaseInfDTOS);
            //筛选计算电站并更新下游关系
            sortedStations = CalDevelopmentProcess.updateDownRelation(sortedStations);
            for (StationBaseInfDTO dto : sortedStations) {
                StationData stationData = hydropowerStationService.changeToStationData(hydropowerStationService.get(dto.getStationName()));
                stationData.setLowerStation(dto.getLowerStation());
                stationDataList.add(stationData);
            }
            stationDataMap.put(calBasin, stationDataList);
        }
        return stationDataMap;
    }


}
