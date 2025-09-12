package com.hust.generatingcapacity.repository;

import com.hust.generatingcapacity.dto.StationInfDTO;
import com.hust.generatingcapacity.entity.HydropowerStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HydropowerStationRepository extends JpaRepository<HydropowerStation, Integer> {

    @Query("SELECT new com.hust.generatingcapacity.dto.StationInfDTO(s.stationName, s.stationCode,s.basin,s.longitude,s.latitude,s.status,s.installedCapacity,s.regulationPerformance," +
            "s.transmissionSection,s.upperStation,s.lowerStation,s.isUnderDdh,s.isStaySichuan,s.isParticipateMarket) " +
            "FROM HydropowerStation s " +
            "WHERE s.stationName = :station_name")
    StationInfDTO findByStationName(@Param("station_name") String stationName);
}
