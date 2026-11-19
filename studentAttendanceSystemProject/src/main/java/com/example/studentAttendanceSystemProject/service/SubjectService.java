package com.example.studentAttendanceSystemProject.service;

import java.util.List;

import com.example.studentAttendanceSystemProject.entity.Department;
import com.example.studentAttendanceSystemProject.entity.Subject;
import com.example.studentAttendanceSystemProject.entity.Teacher;

public interface SubjectService {

	 void save(Subject subject);
	    List<Subject> getAll();
	    void delete(Long id);
		Subject getById(Long id);
		List<Subject> getByTeacher(Teacher teacher);
//		List<Subject> findByTeacher(Teacher teacher);
		List<Subject> findByTeacher(Teacher teacher);
		List<Subject> findByDepartmentAndYear(Department department, String year);
}
