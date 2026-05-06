package com.example.achievement.repository;

import com.example.achievement.entity.Target;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TargetRepository extends JpaRepository<Target, String> {

    List<Target> findByYear(Integer year);

    List<Target> findByOrganization(String organization);

    List<Target> findByOwner(String owner);

    List<Target> findByYearAndOrganization(Integer year, String organization);

    List<Target> findByYearAndOwner(Integer year, String owner);

    @Query("SELECT t FROM Target t WHERE t.year = :year ORDER BY t.organization, t.department")
    List<Target> findByYearOrderByOrganization(@Param("year") Integer year);

    @Query("SELECT DISTINCT t.organization FROM Target t WHERE t.year = :year ORDER BY t.organization")
    List<String> findDistinctOrganizationsByYear(@Param("year") Integer year);

    @Query("SELECT DISTINCT t.owner FROM Target t WHERE t.year = :year ORDER BY t.owner")
    List<String> findDistinctOwnersByYear(@Param("year") Integer year);
}
