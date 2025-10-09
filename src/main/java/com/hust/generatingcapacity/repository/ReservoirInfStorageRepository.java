package com.hust.generatingcapacity.repository;

import com.hust.generatingcapacity.dto.CodeValueDTO;
import com.hust.generatingcapacity.entity.ReservoirInfStorage;
import com.hust.generatingcapacity.model.generation.domain.CodeValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReservoirInfStorageRepository extends JpaRepository<ReservoirInfStorage, Integer> {
    @Query("SELECT new com.hust.generatingcapacity.dto.CodeValueDTO(r.elevation,r.capacity) " +
            "FROM ReservoirInfStorage r " +
            "WHERE r.reservoirInf.id = :reservoir_id")
    List<CodeValueDTO> findAllByReservoirId(@Param("reservoir_id") Integer reservoirId);
}
