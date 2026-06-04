package com.example.achievement.repository;

import com.example.achievement.entity.DtDepartmentBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DtDepartmentBudgetRepository extends JpaRepository<DtDepartmentBudget, String> {

    List<DtDepartmentBudget> findByDeptKey(String deptKey);
}
