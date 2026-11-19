package com.example.studentAttendanceSystemProject.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.studentAttendanceSystemProject.entity.Department;
import com.example.studentAttendanceSystemProject.entity.Subject;
import com.example.studentAttendanceSystemProject.entity.Teacher;


@Repository
public interface SubjectRepository extends JpaRepository<Subject,Long> {

	List<Subject> findByTeacher(Teacher teacher);

	List<Subject> findByDepartmentName(String department);

	List<Subject> findByDepartmentAndYear(Department department, String year);

}
