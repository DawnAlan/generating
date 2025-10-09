package com.hust.generatingcapacity.repository;

import com.hust.generatingcapacity.dto.ConstraintInfDTO;
import com.hust.generatingcapacity.entity.DispatchConstraint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DispatchConstraintRepository extends JpaRepository<DispatchConstraint, Integer> {
    @Query("SELECT new com.hust.generatingcapacity.dto.ConstraintInfDTO(d.id,d.constraintType,d.description,d.isRigid) " +
            "FROM DispatchConstraint d " +
            "WHERE d.hydropowerStation.stationName = :station_name AND d.isActive = true")
    List<ConstraintInfDTO> findAllByStationName(@Param("station_name") String station_name);
}
