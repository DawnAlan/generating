package com.hust.generatingcapacity.iservice;

import com.hust.generatingcapacity.model.entity.CodeValue;

import java.util.List;

public interface IWaterLevelConsumptionService {
    List<CodeValue> get(String station_name);
}
