package com.example.studentAttendanceSystemProject.service;

import java.util.List;

import com.example.studentAttendanceSystemProject.entity.Department;

public interface DepartmentService {

	 void save(Department dept);
	    List<Department> getAll();
	    Department getById(Long id);
	    void delete(Long id);
		List<Department> getAllDepartments();
}
