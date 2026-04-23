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
           "(:status IS NULL OR status = :status) AND " +
           "(:achievementForm IS NULL OR achievement_form = :achievementForm) AND " +
           "(:productId IS NULL OR product_id = :productId) AND " +
           "(:keyword IS NULL OR name LIKE '%' || :keyword || '%' OR description LIKE '%' || :keyword || '%' OR id LIKE '%' || :keyword || '%') AND " +
           "(:includeDeleted = 1 OR status <> 'DELETED') " +
           "ORDER BY created_at DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Achievement> findByConditionsNative(
            @Param("status") String status,
            @Param("achievementForm") String achievementForm,
            @Param("productId") String productId,
            @Param("keyword") String keyword,
            @Param("includeDeleted") int includeDeleted,
            @Param("limit") int limit,
            @Param("offset") int offset);

    @Query(value = "SELECT COUNT(*) FROM achievements WHERE " +
           "(:status IS NULL OR status = :status) AND " +
           "(:achievementForm IS NULL OR achievement_form = :achievementForm) AND " +
           "(:productId IS NULL OR product_id = :productId) AND " +
           "(:keyword IS NULL OR name LIKE '%' || :keyword || '%' OR description LIKE '%' || :keyword || '%' OR id LIKE '%' || :keyword || '%') AND " +
           "(:includeDeleted = 1 OR status <> 'DELETED')", nativeQuery = true)
    long countByConditionsNative(
            @Param("status") String status,
            @Param("achievementForm") String achievementForm,
            @Param("productId") String productId,
            @Param("keyword") String keyword,
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
}
