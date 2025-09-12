package com.hust.generatingcapacity.repository;


import com.hust.generatingcapacity.dto.ReservoirInfDTO;
import com.hust.generatingcapacity.entity.ReservoirInf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservoirInfRepository extends JpaRepository<ReservoirInf, Integer> {
    @Query("SELECT new com.hust.generatingcapacity.dto.ReservoirInfDTO(r.id,r.reservoirName,r.totalCapacity,r.effectiveCapacity,r.deadCapacity,r.normalWaterLevel,r.checkFloodLevel,r.minRegulateLevel) " +
            "FROM ReservoirInf r " +
            "WHERE r.hydropowerStation.stationName  = :station_name")
    ReservoirInfDTO findReservoirInfByStationName(@Param("station_name") String stationName);
}
