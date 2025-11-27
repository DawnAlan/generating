package com.hust.generatingcapacity.iservice;

import com.hust.generatingcapacity.dto.StationBaseInfDTO;
import com.hust.generatingcapacity.dto.StationInfDTO;
import com.hust.generatingcapacity.model.generation.domain.StationData;

import java.util.List;

public interface IHydropowerStationService {
    StationInfDTO get(String stationName);
    StationData changeToStationData(StationInfDTO stationInfDTO);
    List<StationBaseInfDTO> getBasinStations(String basinName);
}
