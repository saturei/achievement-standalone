package com.example.achievement.repository;

import com.example.achievement.entity.DtProjectOrderCost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DtProjectOrderCostRepository extends JpaRepository<DtProjectOrderCost, String> {

    List<DtProjectOrderCost> findByDepartment(String department);

    List<DtProjectOrderCost> findByProjectId(String projectId);

    List<DtProjectOrderCost> findByProductId(String productId);

    List<DtProjectOrderCost> findByOrderId(String orderId);
}
