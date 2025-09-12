package com.hust.generatingcapacity.repository;

import com.hust.generatingcapacity.dto.UnitInfDTO;
import com.hust.generatingcapacity.entity.UnitInf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UnitInfRepository extends JpaRepository<UnitInf,Integer> {
    @Query("SELECT new com.hust.generatingcapacity.dto.UnitInfDTO(u.id,u.unitName,u.unitCapacity,u.status,u.unitInfNhqCurve.curveName) " +
            "FROM UnitInf u " +
            "WHERE u.hydropowerStation.stationName = :station_name")
    List<UnitInfDTO> findAllByStationName(@Param("station_name") String stationName);

    //通过电站名称找寻该电站所有的机组id
    @Query("SELECT u.id " +
            "FROM UnitInf u " +
            "WHERE u.hydropowerStation.stationName = :station_name")
    List<Integer> findInfIdsByStationName(@Param("station_name") String stationName);

    @Query("SELECT u.unitInfNhqCurve.id " +
            "FROM UnitInf u " +
            "WHERE u.id = :inf_id")
    Integer findCurveIdByInfId(@Param("inf_id") Integer inf_id);
}
