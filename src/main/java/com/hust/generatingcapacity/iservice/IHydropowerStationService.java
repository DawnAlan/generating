package com.hust.generatingcapacity.iservice;

import com.hust.generatingcapacity.dto.StationInfDTO;
import com.hust.generatingcapacity.model.generation.domain.StationData;

public interface IHydropowerStationService {
    StationInfDTO get(String stationName);
    StationData changeToStationData(StationInfDTO stationInfDTO);
}
