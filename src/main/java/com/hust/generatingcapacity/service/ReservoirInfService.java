package com.hust.generatingcapacity.service;

import com.hust.generatingcapacity.dto.ReservoirInfDTO;
import com.hust.generatingcapacity.iservice.IReservoirInfService;
import com.hust.generatingcapacity.model.entity.CodeValue;
import com.hust.generatingcapacity.repository.ReservoirInfRepository;
import com.hust.generatingcapacity.repository.ReservoirInfStorageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReservoirInfService implements IReservoirInfService {
    @Autowired
    ReservoirInfRepository reservoirInfRepository;
    @Autowired
    ReservoirInfStorageRepository reservoirInfStorageRepository;

    @Override
    public ReservoirInfDTO get(String stationName) {
        ReservoirInfDTO result = reservoirInfRepository.findReservoirInfByStationName(stationName);
        List<CodeValue> storageLine = reservoirInfStorageRepository.findAllByReservoirId(result.getId());
        result.setReservoirStorageLine(storageLine);
        return result;
    }
}
