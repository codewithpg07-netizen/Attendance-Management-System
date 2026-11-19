package com.example.studentAttendanceSystemProject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.studentAttendanceSystemProject.entity.AttendanceSettings;

@Repository
public interface AttendanceSettingsRepository extends JpaRepository<AttendanceSettings, Long> {
}
