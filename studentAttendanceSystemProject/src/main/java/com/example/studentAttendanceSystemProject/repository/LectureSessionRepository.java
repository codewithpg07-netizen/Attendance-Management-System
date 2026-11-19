package com.example.studentAttendanceSystemProject.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.studentAttendanceSystemProject.entity.LectureSession;
import com.example.studentAttendanceSystemProject.entity.Subject;
import com.example.studentAttendanceSystemProject.entity.Teacher;

@Repository
public interface LectureSessionRepository extends JpaRepository<LectureSession, Long> {

    Optional<LectureSession> findByQrToken(String qrToken);

    Optional<LectureSession> findBySessionCode(String sessionCode);

    List<LectureSession> findByTeacherOrderByCreatedAtDesc(Teacher teacher);

    List<LectureSession> findBySubjectOrderByDateDesc(Subject subject);

    List<LectureSession> findByDate(LocalDate date);

    List<LectureSession> findByTeacherAndDate(Teacher teacher, LocalDate date);

    List<LectureSession> findByStatus(String status);
}
