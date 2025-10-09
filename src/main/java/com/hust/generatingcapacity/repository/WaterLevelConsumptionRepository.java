package com.hust.generatingcapacity.repository;

import com.hust.generatingcapacity.dto.CodeValueDTO;
import com.hust.generatingcapacity.entity.WaterLevelConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WaterLevelConsumptionRepository extends JpaRepository<WaterLevelConsumption, Integer> {
    @Query("SELECT new com.hust.generatingcapacity.dto.CodeValueDTO(w.waterLevel,w.consumption) " +
            "FROM WaterLevelConsumption w " +
            "WHERE w.hydropowerStation.stationName = :station_name")
    List<CodeValueDTO> findAllByStationName(@Param("station_name") String station_name);
}
