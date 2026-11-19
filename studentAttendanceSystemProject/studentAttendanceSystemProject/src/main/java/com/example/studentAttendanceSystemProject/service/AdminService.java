package com.example.studentAttendanceSystemProject.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.example.studentAttendanceSystemProject.entity.PreVerifiedStudent;
import com.example.studentAttendanceSystemProject.entity.User;

public interface AdminService {
	
	void uploadStudentList(MultipartFile file) throws Exception;
	 void addTeacher(User teacher);
	    List<User> getAllTeachers();
	    List<PreVerifiedStudent> getAllStudents();
	    PreVerifiedStudent getStudentById(Long id);
	    PreVerifiedStudent saveStudent(PreVerifiedStudent student);
	    void deleteStudent(Long id);
	    
	   
	    List<PreVerifiedStudent> getFilteredStudents(Long departmentId, String year);

}
