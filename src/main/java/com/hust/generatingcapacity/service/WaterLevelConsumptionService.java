package com.hust.generatingcapacity.service;

import com.hust.generatingcapacity.dto.CodeValueDTO;
import com.hust.generatingcapacity.iservice.IWaterLevelConsumptionService;
import com.hust.generatingcapacity.repository.WaterLevelConsumptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WaterLevelConsumptionService implements IWaterLevelConsumptionService {

    @Autowired
    WaterLevelConsumptionRepository waterLevelConsumptionRepository;

    @Override
    public List<CodeValueDTO> get(String station_name) {
        return waterLevelConsumptionRepository.findAllByStationName(station_name);
    }

    @Override
    public Boolean getIsWaterConsumptionLine(String station_name) {
        List<CodeValueDTO> list = waterLevelConsumptionRepository.findAllByStationName(station_name);
        return list != null && !list.isEmpty();
    }

}
