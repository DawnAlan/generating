package com.hust.generatingcapacity.iservice;

import com.hust.generatingcapacity.dto.ConstraintInfDTO;

import java.util.List;

public interface IDispatchConstraintService {
    List<ConstraintInfDTO> get(String stationName);
}
