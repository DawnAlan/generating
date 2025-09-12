package com.hust.generatingcapacity.iservice;

import com.hust.generatingcapacity.dto.UnitInfDTO;

import java.util.List;

public interface IUnitInfService {
    List<UnitInfDTO> get(String stationName);
}
