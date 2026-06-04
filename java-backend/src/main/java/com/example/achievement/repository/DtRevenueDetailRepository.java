package com.example.achievement.repository;

import com.example.achievement.entity.DtRevenueDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DtRevenueDetailRepository extends JpaRepository<DtRevenueDetail, String> {

    List<DtRevenueDetail> findByOrderId(String orderId);
}
