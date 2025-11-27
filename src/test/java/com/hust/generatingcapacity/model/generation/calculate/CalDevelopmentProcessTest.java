package com.hust.generatingcapacity.model.generation.calculate;

import com.hust.generatingcapacity.GeneratingCapacityApplication;
import com.hust.generatingcapacity.dto.StationBaseInfDTO;
import com.hust.generatingcapacity.iservice.IHydropowerStationService;
import com.hust.generatingcapacity.model.generation.domain.StationData;
import com.hust.generatingcapacity.repository.HydropowerStationRepository;
import com.hust.generatingcapacity.service.HydropowerStationService;
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
            for (StationBaseInfDTO dto : sortedStations) {
                StationData stationData = hydropowerStationService.changeToStationData(hydropowerStationService.get(dto.getStationName()));
                stationData.setLowerStation(dto.getLowerStation());
                stationDataList.add(stationData);
            }
            stationDataMap.put(calBasin, stationDataList);
        }

        System.out.println("done");
        //完成各流域电站的收集和排序,进行后续输入数据处理
    }


}
