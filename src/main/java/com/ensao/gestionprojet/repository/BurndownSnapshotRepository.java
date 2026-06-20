package com.ensao.gestionprojet.repository;

import com.ensao.gestionprojet.entity.BurndownSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BurndownSnapshotRepository extends JpaRepository<BurndownSnapshot, Long> {
    List<BurndownSnapshot> findBySprintIdOrderByDateAsc(Long sprintId);
}
