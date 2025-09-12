package com.hust.generatingcapacity.datebase;

import com.hust.generatingcapacity.dto.StationInfDTO;
import com.hust.generatingcapacity.iservice.IHydropowerStationService;
import com.hust.generatingcapacity.service.HydropowerStationService;


public class ConnectionTest {
    public static void main(String[] args) {
        StationInfDTO stationInfDTO = new HydropowerStationService().get("猴子岩");
        System.out.println(stationInfDTO);

    }
}
