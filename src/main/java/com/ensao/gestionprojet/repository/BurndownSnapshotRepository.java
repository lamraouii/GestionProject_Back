package com.ensao.gestionprojet.repository;

import com.ensao.gestionprojet.entity.BurndownSnapshot;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BurndownSnapshotRepository {
    List<BurndownSnapshot> findBySprintIdOrderByDateAsc(Long sprintId);

    void save(BurndownSnapshot snapshot);
}
