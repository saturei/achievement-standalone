package com.example.achievement.repository;

import com.example.achievement.entity.RevenueRecognition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RevenueRecognitionRepository extends JpaRepository<RevenueRecognition, String> {

    List<RevenueRecognition> findByOrganizationAndYear(String organization, Integer year);

    List<RevenueRecognition> findByYear(Integer year);

    List<RevenueRecognition> findByRecognitionMonthStartingWith(String month);

    List<RevenueRecognition> findByOrganizationAndRecognitionMonthStartingWith(String organization, String month);

    List<RevenueRecognition> findByContractIdAndOrderId(String contractId, String orderId);

    @Query(value = "SELECT organization, year, quarter, sub_category, SUM(recognition_amount), SUM(revenue_amount) FROM revenue_recognitions " +
           "WHERE sub_category IS NOT NULL GROUP BY organization, year, quarter, sub_category ORDER BY organization, year, quarter", nativeQuery = true)
    List<Object[]> sumAmountsByOrgYearQuarter();

    @Query(value = "SELECT SUM(recognition_amount) FROM revenue_recognitions WHERE organization = :org AND year = :year AND quarter = :quarter", nativeQuery = true)
    java.math.BigDecimal sumRecognitionAmountByOrgYearQuarter(@Param("org") String org, @Param("year") int year, @Param("quarter") int quarter);

    @Query(value = "SELECT SUM(revenue_amount) FROM revenue_recognitions WHERE organization = :org AND year = :year AND quarter = :quarter", nativeQuery = true)
    java.math.BigDecimal sumRevenueAmountByOrgYearQuarter(@Param("org") String org, @Param("year") int year, @Param("quarter") int quarter);
}
