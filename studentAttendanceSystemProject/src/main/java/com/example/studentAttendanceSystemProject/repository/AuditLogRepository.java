package com.example.studentAttendanceSystemProject.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.studentAttendanceSystemProject.entity.AuditLog;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findTop100ByOrderByTimestampDesc();

    List<AuditLog> findByActionOrderByTimestampDesc(String action);

    List<AuditLog> findByUsernameOrderByTimestampDesc(String username);
}
