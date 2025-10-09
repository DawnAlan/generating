package com.hust.generatingcapacity.service;

import com.hust.generatingcapacity.GeneratingCapacityApplication;
import com.hust.generatingcapacity.dto.StationInfDTO;
import com.hust.generatingcapacity.iservice.IHydropowerStationService;
import com.hust.generatingcapacity.model.generation.domain.StationData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = GeneratingCapacityApplication.class)
public class HydropowerStationServiceTest {

    @Autowired
    private IHydropowerStationService hydropowerStationService;

    @Test
    void testGetStation() {
        StationInfDTO dto = hydropowerStationService.get("猴子岩");
        StationData stationData = hydropowerStationService.changeToStationData(dto);
        System.out.println(stationData);
    }
}
