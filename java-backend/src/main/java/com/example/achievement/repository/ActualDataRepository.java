package com.example.achievement.repository;

import com.example.achievement.entity.ActualData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActualDataRepository extends JpaRepository<ActualData, String> {

    List<ActualData> findByTargetId(String targetId);

    List<ActualData> findByYear(Integer year);

    List<ActualData> findByYearAndMonth(Integer year, Integer month);

    List<ActualData> findByOrganization(String organization);

    List<ActualData> findByYearAndOrganization(Integer year, String organization);

    @Query("SELECT a FROM ActualData a WHERE a.targetId = :targetId ORDER BY a.year, a.month")
    List<ActualData> findByTargetIdOrderByYearAndMonth(@Param("targetId") String targetId);

    @Query("SELECT SUM(a.actualValue) FROM ActualData a WHERE a.targetId = :targetId AND a.year = :year")
    Double sumActualValueByTargetIdAndYear(@Param("targetId") String targetId, @Param("year") Integer year);

    @Query("SELECT SUM(a.actualValue) FROM ActualData a WHERE a.targetId = :targetId AND a.year = :year AND a.month BETWEEN :startMonth AND :endMonth")
    Double sumActualValueByTargetIdAndYearAndMonthBetween(
            @Param("targetId") String targetId, 
            @Param("year") Integer year, 
            @Param("startMonth") Integer startMonth, 
            @Param("endMonth") Integer endMonth);
}
