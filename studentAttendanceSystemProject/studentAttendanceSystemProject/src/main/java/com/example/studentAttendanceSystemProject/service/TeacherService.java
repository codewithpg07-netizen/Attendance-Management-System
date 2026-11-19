package com.example.studentAttendanceSystemProject.service;

import java.util.List;
import java.util.Optional;

import com.example.studentAttendanceSystemProject.entity.Teacher;

public interface TeacherService {

	 void addTeacher(Teacher teacher);

	List<Teacher> getAllTeachers();
	 Teacher getTeacherById(Long id);
	    Teacher updateTeacher(Teacher teacher);
	    void deleteTeacher(Long id);

		Teacher getByEmail(String email);

		Teacher getById(Long id);


		

		
}
