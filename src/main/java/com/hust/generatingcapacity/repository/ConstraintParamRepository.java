package com.hust.generatingcapacity.repository;

import com.hust.generatingcapacity.entity.ConstraintParam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConstraintParamRepository extends JpaRepository<ConstraintParam,Integer> {
    @Query("SELECT c FROM ConstraintParam c " +
            "WHERE c.dispatchConstraint.id = :constraint_id")
    List<ConstraintParam> findByConstraintId(@Param("constraint_id") Integer constraint_id);
}
