package com.hust.generatingcapacity.iservice;

import com.hust.generatingcapacity.dto.CodeValueDTO;
import com.hust.generatingcapacity.model.generation.domain.CodeValue;

import java.util.List;

public interface ITailWaterLevelFlowService {
    List<CodeValueDTO> get(String station_name);
}
