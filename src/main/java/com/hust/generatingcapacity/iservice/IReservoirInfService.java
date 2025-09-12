package com.hust.generatingcapacity.iservice;

import com.hust.generatingcapacity.dto.ReservoirInfDTO;

public interface IReservoirInfService {
    ReservoirInfDTO get(String stationName);
}
