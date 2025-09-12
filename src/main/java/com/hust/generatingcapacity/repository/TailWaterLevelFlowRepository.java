package com.hust.generatingcapacity.repository;

import com.hust.generatingcapacity.entity.TailWaterLevelFlow;
import com.hust.generatingcapacity.model.entity.CodeValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TailWaterLevelFlowRepository extends JpaRepository<TailWaterLevelFlow,Integer> {
    @Query("SELECT new com.hust.generatingcapacity.model.entity.CodeValue(t.outFlow,t.waterLevel) " +
            "FROM TailWaterLevelFlow t " +
            "WHERE t.hydropowerStation.stationName = :station_name")
    List<CodeValue> findAllByStationName(@Param("station_name") String station_name);
}
