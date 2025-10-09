package com.hust.generatingcapacity.service;

import com.hust.generatingcapacity.dto.*;
import com.hust.generatingcapacity.iservice.*;
import com.hust.generatingcapacity.model.generation.domain.NHQCell;
import com.hust.generatingcapacity.model.generation.domain.NHQData;
import com.hust.generatingcapacity.model.generation.domain.StationData;
import com.hust.generatingcapacity.model.generation.util.DTOMapper;
import com.hust.generatingcapacity.repository.HydropowerStationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
    @Autowired
    DTOMapper dtoMapper;

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
        List<CodeValueDTO> waterConsumptionLine = waterLevelConsumptionService.get(stationName);
        //获取流量尾水位曲线
        List<CodeValueDTO> tailLevelFlowLine = tailWaterLevelFlowService.get(stationName);
        result.setWaterConsumptionLine(waterConsumptionLine);
        result.setTailLevelFlowLine(tailLevelFlowLine);
        result.setReservoirInf(reservoirInfDTO);
        result.setUnitInfs(unitInfs);
        result.setConstraintInfs(constraintInfs);
        return result;
    }

    @Override
    public StationData changeToStationData(StationInfDTO stationInfDTO) {
        StationData stationData = new StationData(stationInfDTO);
        if (stationInfDTO.getReservoirInf() != null && stationInfDTO.getReservoirInf().getReservoirStorageLine() != null) {
            stationData.setReservoirStorageLine(dtoMapper.toCodeValueList(stationInfDTO.getReservoirInf().getReservoirStorageLine()));
        }
        if (stationInfDTO.getWaterConsumptionLine() != null) {
            stationData.setWaterConsumptionLine(dtoMapper.toCodeValueList(stationInfDTO.getWaterConsumptionLine()));
        }
        if (stationInfDTO.getTailLevelFlowLine() != null) {
            stationData.setTailLevelFlowLine(dtoMapper.toCodeValueList(stationInfDTO.getTailLevelFlowLine()));
        }
        if (stationInfDTO.getConstraintInfs() != null && !stationInfDTO.getConstraintInfs().isEmpty()) {
            stationData.setConstraints(dtoMapper.toConstraintDataList(stationInfDTO.getConstraintInfs()));
        }
        if (stationInfDTO.getUnitInfs() != null) {
            List<List<NHQData>> nhqDataList = new ArrayList<>();
            for (int i = 0; i < stationInfDTO.getUnitInfs().size(); i++) {
                List<NHQData> nhqData = NHQCell.convert(dtoMapper.toNHQCellList(stationInfDTO.getUnitInfs().get(i).getNhqLine()));
                nhqDataList.add(nhqData);
            }
            stationData.setNHQLines(nhqDataList);
        }
        return stationData;
    }
}
