package com.example.studentAttendanceSystemProject.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.studentAttendanceSystemProject.entity.PreVerifiedStudent;
import com.example.studentAttendanceSystemProject.entity.SmsAlertLog;

@Repository
public interface SmsAlertLogRepository extends JpaRepository<SmsAlertLog, Long> {

    List<SmsAlertLog> findTop100ByOrderBySentAtDesc();

    List<SmsAlertLog> findByStudentOrderBySentAtDesc(PreVerifiedStudent student);

    List<SmsAlertLog> findByMonthYear(String monthYear);
}
