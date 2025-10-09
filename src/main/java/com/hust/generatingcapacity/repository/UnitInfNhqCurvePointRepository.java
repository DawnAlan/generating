package com.hust.generatingcapacity.repository;

import com.hust.generatingcapacity.dto.NHQCellDTO;
import com.hust.generatingcapacity.entity.UnitInfNhqCurvePoint;
import com.hust.generatingcapacity.model.generation.domain.NHQCell;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UnitInfNhqCurvePointRepository extends JpaRepository<UnitInfNhqCurvePoint, Integer> {
    @Query("SELECT new com.hust.generatingcapacity.dto.NHQCellDTO(u.head,u.flow,u.power,u.maxPower,u.minPower,u.maxFlow,u.minFlow) " +
            "FROM UnitInfNhqCurvePoint u " +
            "WHERE u.unitInfNhqCurve.id = :curve_id")
    List<NHQCellDTO> findAllByCurveId(@Param("curve_id") Integer curveId);
}
