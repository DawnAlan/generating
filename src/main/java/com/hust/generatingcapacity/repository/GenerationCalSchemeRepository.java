package com.hust.generatingcapacity.repository;


import com.hust.generatingcapacity.entity.GenerationCalScheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GenerationCalSchemeRepository extends JpaRepository<GenerationCalScheme, Integer> {
    @Query("SELECT s.id  " +
            "FROM GenerationCalScheme s " +
            "WHERE s.schemeName = :name")
    Optional<Integer> findIdByName(@Param("name")String name);
}
