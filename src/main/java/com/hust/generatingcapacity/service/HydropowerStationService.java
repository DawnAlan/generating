package com.hust.generatingcapacity.service;

import com.hust.generatingcapacity.dto.ConstraintInfDTO;
import com.hust.generatingcapacity.dto.ReservoirInfDTO;
import com.hust.generatingcapacity.dto.StationInfDTO;
import com.hust.generatingcapacity.dto.UnitInfDTO;
import com.hust.generatingcapacity.iservice.*;
import com.hust.generatingcapacity.model.entity.CodeValue;
import com.hust.generatingcapacity.repository.HydropowerStationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HydropowerStationService implements IHydropowerStationService {
    @Autowired
    HydropowerStationRepository hydropowerStationRepository;
    @Autowired
    IWaterLevelConsumptionService waterLevelConsumptionService;
    @Autowired
    ITailWaterLevelFlowService tailWaterLevelFlowService;
    @Autowired
    IDispatchConstraintService dispatchConstraintService;
    @Autowired
    IReservoirInfService reservoirInfService;
    @Autowired
    IUnitInfService unitInfService;

    @Override
    public StationInfDTO get(String stationName) {
        StationInfDTO result = hydropowerStationRepository.findByStationName(stationName);
        //获取水库特征信息
        ReservoirInfDTO reservoirInfDTO = reservoirInfService.get(stationName);
        //获取机组信息
        List<UnitInfDTO> unitInfs = unitInfService.get(stationName);
        //获取约束信息
        List<ConstraintInfDTO> constraintInfs = dispatchConstraintService.get(stationName);
        //获取水位耗水率曲线
        List<CodeValue> waterConsumptionLine = waterLevelConsumptionService.get(stationName);
        //获取流量尾水位曲线
        List<CodeValue> tailLevelFlowLine = tailWaterLevelFlowService.get(stationName);
        result.setWaterConsumptionLine(waterConsumptionLine);
        result.setTailLevelFlowLine(tailLevelFlowLine);
        result.setReservoirInf(reservoirInfDTO);
        result.setUnitInfs(unitInfs);
        result.setConstraintInfs(constraintInfs);
        return result;
    }
}
