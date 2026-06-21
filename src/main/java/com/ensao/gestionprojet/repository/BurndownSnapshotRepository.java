package com.ensao.gestionprojet.repository;

import com.ensao.gestionprojet.entity.BurndownSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BurndownSnapshotRepository extends JpaRepository<BurndownSnapshot, Long> {
    List<BurndownSnapshot> findBySprintIdOrderByDateAsc(Long sprintId);

    Optional<BurndownSnapshot> findBySprintIdAndDate(Long sprintId, java.time.LocalDate date);
}
