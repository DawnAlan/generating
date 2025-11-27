package com.hust.generatingcapacity.repository;

import com.hust.generatingcapacity.dto.StationBaseInfDTO;
import com.hust.generatingcapacity.dto.StationInfDTO;
import com.hust.generatingcapacity.entity.HydropowerStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HydropowerStationRepository extends JpaRepository<HydropowerStation, Integer> {

    @Query("SELECT new com.hust.generatingcapacity.dto.StationInfDTO(s.stationName, s.stationCode,s.basin,s.longitude,s.latitude,s.status,s.installedCapacity,s.regulationPerformance," +
            "s.transmissionSection,s.upperStation,s.lowerStation,s.isUnderDdh,s.isProceduralCalculation,s.isParticipateMarket) " +
            "FROM HydropowerStation s " +
            "WHERE s.stationName = :station_name")
    StationInfDTO findByStationName(@Param("station_name") String stationName);

    @Query("SELECT new com.hust.generatingcapacity.dto.StationBaseInfDTO(s.stationName, s.basin,s.status,s.installedCapacity,s.upperStation,s.lowerStation,s.isProceduralCalculation,s.isParticipateMarket) " +
            "FROM HydropowerStation s " +
            "WHERE s.stationName = :station_name")
    StationBaseInfDTO findBaseByStationName(@Param("station_name") String stationName);

    @Query("SELECT new com.hust.generatingcapacity.dto.StationBaseInfDTO(s.stationName, s.basin,s.status,s.installedCapacity,s.upperStation,s.lowerStation,s.isProceduralCalculation,s.isParticipateMarket) " +
            "FROM HydropowerStation s " +
            "WHERE s.basin = :basin")
    List<StationBaseInfDTO> findAllBaseByStationName(@Param("basin") String basin);

    @Query("SELECT s.stationName " +
            "FROM HydropowerStation s " +
            "WHERE s.basin = :basin")
    List<String> findAllStationByBasin(@Param("basin") String basin);

    @Query("SELECT s.stationName " +
            "FROM HydropowerStation s " +
            "WHERE s.lowerStation = :lower_station")
    List<String> findUpperStationByLowerStation(@Param("lower_station") String lowerStation);

    @Query("SELECT s.stationName " +
            "FROM HydropowerStation s " +
            "WHERE s.lowerStation = :lower_station AND s.basin != :basin")
    List<String> findUpperStationByLowerStationDifferentBasin(@Param("lower_station") String lowerStation, @Param("basin") String basin);
}
