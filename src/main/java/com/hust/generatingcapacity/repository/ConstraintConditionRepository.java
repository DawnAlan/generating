package com.hust.generatingcapacity.repository;

import com.hust.generatingcapacity.entity.ConstraintCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConstraintConditionRepository extends JpaRepository<ConstraintCondition, Integer> {
    @Query("SELECT c FROM ConstraintCondition c " +
            "WHERE c.dispatchConstraint.id = :constraint_id")
    List<ConstraintCondition> findByConstraintId(@Param("constraint_id") Integer constraint_id);

}
