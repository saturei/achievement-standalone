package com.example.achievement.repository;

import com.example.achievement.entity.Achievement;
import com.example.achievement.enums.AchievementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, String> {

    @Query(value = "SELECT * FROM achievements WHERE achievement_form = :achievementForm ORDER BY created_at DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Achievement> findByAchievementFormNative(@Param("achievementForm") String achievementForm, @Param("limit") int limit, @Param("offset") int offset);

    @Query(value = "SELECT * FROM achievements WHERE " +
           "(:keyword IS NULL OR name LIKE '%' || :keyword || '%' OR description LIKE '%' || :keyword || '%' OR id LIKE '%' || :keyword || '%') AND " +
           "(:departmentName IS NULL OR department_name = :departmentName) AND " +
           "(:organizationNames IS NULL OR ',' || :organizationNames || ',' LIKE '%,' || organization_name || ',%') AND " +
           "(:status IS NULL OR status = :status) AND " +
           "(:productId IS NULL OR product_id = :productId) AND " +
           "(:includeDeleted = 1 OR status <> 'DELETED') " +
           "ORDER BY created_at DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Achievement> findByConditionsNative(
            @Param("keyword") String keyword,
            @Param("departmentName") String departmentName,
            @Param("organizationNames") String organizationNames,
            @Param("status") String status,
            @Param("productId") String productId,
            @Param("includeDeleted") int includeDeleted,
            @Param("limit") int limit,
            @Param("offset") int offset);

    @Query(value = "SELECT COUNT(*) FROM achievements WHERE " +
           "(:keyword IS NULL OR name LIKE '%' || :keyword || '%' OR description LIKE '%' || :keyword || '%' OR id LIKE '%' || :keyword || '%') AND " +
           "(:departmentName IS NULL OR department_name = :departmentName) AND " +
           "(:organizationNames IS NULL OR ',' || :organizationNames || ',' LIKE '%,' || organization_name || ',%') AND " +
           "(:status IS NULL OR status = :status) AND " +
           "(:productId IS NULL OR product_id = :productId) AND " +
           "(:includeDeleted = 1 OR status <> 'DELETED')", nativeQuery = true)
    long countByConditionsNative(
            @Param("keyword") String keyword,
            @Param("departmentName") String departmentName,
            @Param("organizationNames") String organizationNames,
            @Param("status") String status,
            @Param("productId") String productId,
            @Param("includeDeleted") int includeDeleted);

    List<Achievement> findByNameAndProductExternalVersionOrderByCreatedAtDesc(
            String name, String productExternalVersion);

    @Query(value = "SELECT * FROM achievements WHERE name = :name AND product_external_version = :productExternalVersion ORDER BY created_at DESC LIMIT 1", nativeQuery = true)
    Optional<Achievement> findTopByNameAndProductExternalVersionOrderByCreatedAtDesc(
            @Param("name") String name, @Param("productExternalVersion") String productExternalVersion);

    @Query(value = "SELECT COUNT(*) FROM achievements WHERE status = :status", nativeQuery = true)
    long countByStatus(@Param("status") String status);

    long countByProductId(String productId);

    @Query(value = "SELECT COUNT(*) FROM achievements WHERE status <> 'DELETED'", nativeQuery = true)
    long countAllExcludingDeleted();

    @Query(value = "SELECT DISTINCT organization_name FROM achievements WHERE organization_name IS NOT NULL AND organization_name <> '' AND status <> 'DELETED' ORDER BY organization_name", nativeQuery = true)
    List<String> findDistinctOrganizationNames();

    @Query(value = "SELECT DISTINCT department_name FROM achievements WHERE department_name IS NOT NULL AND department_name <> '' AND status <> 'DELETED' ORDER BY department_name", nativeQuery = true)
    List<String> findDistinctDepartmentNames();

    @Query(value = "SELECT DISTINCT department_name FROM achievements WHERE " +
           "(:keyword IS NULL OR name LIKE '%' || :keyword || '%' OR description LIKE '%' || :keyword || '%' OR id LIKE '%' || :keyword || '%') AND " +
           "(:organizationNames IS NULL OR ',' || :organizationNames || ',' LIKE '%,' || organization_name || ',%') AND " +
           "(:status IS NULL OR status = :status) AND " +
           "(:productId IS NULL OR product_id = :productId) AND " +
           "status <> 'DELETED' AND department_name IS NOT NULL AND department_name <> '' " +
           "ORDER BY department_name", nativeQuery = true)
    List<String> findDistinctDepartmentNamesFiltered(
            @Param("keyword") String keyword,
            @Param("organizationNames") String organizationNames,
            @Param("status") String status,
            @Param("productId") String productId);

    @Query(value = "SELECT DISTINCT organization_name FROM achievements WHERE " +
           "(:keyword IS NULL OR name LIKE '%' || :keyword || '%' OR description LIKE '%' || :keyword || '%' OR id LIKE '%' || :keyword || '%') AND " +
           "(:departmentName IS NULL OR department_name = :departmentName) AND " +
           "(:status IS NULL OR status = :status) AND " +
           "(:productId IS NULL OR product_id = :productId) AND " +
           "status <> 'DELETED' AND organization_name IS NOT NULL AND organization_name <> '' " +
           "ORDER BY organization_name", nativeQuery = true)
    List<String> findDistinctOrganizationNamesFiltered(
            @Param("keyword") String keyword,
            @Param("departmentName") String departmentName,
            @Param("status") String status,
            @Param("productId") String productId);

    @Query(value = "SELECT DISTINCT owner FROM achievements WHERE owner IS NOT NULL AND owner <> '' AND status <> 'DELETED' ORDER BY owner", nativeQuery = true)
    List<String> findDistinctOwners();

    @Query(value = "SELECT DISTINCT type FROM achievements WHERE type IS NOT NULL AND type <> '' AND status <> 'DELETED' ORDER BY type", nativeQuery = true)
    List<String> findDistinctTypes();
}
