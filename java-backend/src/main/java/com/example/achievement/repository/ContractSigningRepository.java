package com.example.achievement.repository;

import com.example.achievement.entity.ContractSigning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractSigningRepository extends JpaRepository<ContractSigning, String> {

    List<ContractSigning> findByOrganizationAndYear(String organization, Integer year);

    List<ContractSigning> findByYear(Integer year);

    List<ContractSigning> findBySignMonthStartingWith(String signMonth);

    List<ContractSigning> findByOrganizationAndSignMonthStartingWith(String organization, String signMonth);

    List<ContractSigning> findByContractIdAndPackageId(String contractId, String packageId);

    @Query(value = "SELECT organization, \"year\", quarter, sub_category, SUM(amount) FROM contract_signings " +
           "WHERE sub_category IS NOT NULL GROUP BY organization, \"year\", quarter, sub_category ORDER BY organization, \"year\", quarter", nativeQuery = true)
    List<Object[]> sumAmountByOrgYearQuarter();

    @Query(value = "SELECT SUM(amount) FROM contract_signings WHERE organization = :org AND \"year\" = :year AND quarter = :quarter", nativeQuery = true)
    java.math.BigDecimal sumAmountByOrgYearQuarter(@Param("org") String org, @Param("year") int year, @Param("quarter") int quarter);
}
