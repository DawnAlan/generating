package com.hust.generatingcapacity.service;

import com.hust.generatingcapacity.dto.UnitInfDTO;
import com.hust.generatingcapacity.iservice.IUnitInfService;
import com.hust.generatingcapacity.repository.UnitInfNhqCurvePointRepository;
import com.hust.generatingcapacity.repository.UnitInfRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UnitInfService implements IUnitInfService {
    @Autowired
    UnitInfRepository unitInfRepository;
    @Autowired
    UnitInfNhqCurvePointRepository unitInfNhqCurvePointRepository;

    @Override
    public List<UnitInfDTO> get(String stationName) {
        List<UnitInfDTO> results = unitInfRepository.findAllByStationName(stationName);
        if (!results.isEmpty()) {
            for (UnitInfDTO unitInfDTO : results) {
                Integer curveId = unitInfRepository.findCurveIdByInfId(unitInfDTO.getId());
                if (curveId != null) {
                    unitInfDTO.setNhqLine(unitInfNhqCurvePointRepository.findAllByCurveId(curveId));
                }
            }
        }
        return results;
    }
}
