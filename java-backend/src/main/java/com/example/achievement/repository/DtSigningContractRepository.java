package com.example.achievement.repository;

import com.example.achievement.entity.DtSigningContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DtSigningContractRepository extends JpaRepository<DtSigningContract, String> {

    List<DtSigningContract> findByContractId(String contractId);
}
