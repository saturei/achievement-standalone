package com.example.achievement.repository;

import com.example.achievement.entity.DtProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DtProductRepository extends JpaRepository<DtProduct, String> {

    List<DtProduct> findByProductId(String productId);
}
