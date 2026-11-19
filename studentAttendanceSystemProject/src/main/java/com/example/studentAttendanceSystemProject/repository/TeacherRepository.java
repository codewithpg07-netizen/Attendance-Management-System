package com.example.studentAttendanceSystemProject.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.studentAttendanceSystemProject.entity.Teacher;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

//	Teacher findByEmail(String email);
	 Optional<Teacher> findByEmail(String email);

}
