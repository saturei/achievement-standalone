package com.example.achievement.repository;

import com.example.achievement.entity.DtSigningOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DtSigningOrderRepository extends JpaRepository<DtSigningOrder, String> {

    List<DtSigningOrder> findByOrderId(String orderId);
}
