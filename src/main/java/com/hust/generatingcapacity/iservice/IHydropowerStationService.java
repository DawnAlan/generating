package com.hust.generatingcapacity.iservice;

import com.hust.generatingcapacity.dto.StationInfDTO;

public interface IHydropowerStationService {
    StationInfDTO get(String stationName);
}
