package com.example.achievement.repository;

import com.example.achievement.entity.DtProductPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DtProductPackageRepository extends JpaRepository<DtProductPackage, String> {

    List<DtProductPackage> findByPackageId(String packageId);
}
