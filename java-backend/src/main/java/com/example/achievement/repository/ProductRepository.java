package com.example.achievement.repository;

import com.example.achievement.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 产品数据访问层
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    /**
     * 根据产品编码查找产品
     *
     * @param productCode 产品编码
     * @return 产品
     */
    Optional<Product> findByProductCode(String productCode);

    /**
     * 根据产品名称查找产品
     *
     * @param productName 产品名称
     * @return 产品
     */
    Optional<Product> findByProductName(String productName);
}
