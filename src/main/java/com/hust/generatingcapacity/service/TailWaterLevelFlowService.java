package com.hust.generatingcapacity.service;

import com.hust.generatingcapacity.dto.CodeValueDTO;
import com.hust.generatingcapacity.iservice.ITailWaterLevelFlowService;
import com.hust.generatingcapacity.model.generation.domain.CodeValue;
import com.hust.generatingcapacity.repository.TailWaterLevelFlowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TailWaterLevelFlowService implements ITailWaterLevelFlowService {
    @Autowired
    TailWaterLevelFlowRepository tailWaterLevelFlowRepository;
    @Override
    public List<CodeValueDTO> get(String station_name) {
        return tailWaterLevelFlowRepository.findAllByStationName(station_name);
    }
}
