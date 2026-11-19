package com.example.studentAttendanceSystemProject.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.studentAttendanceSystemProject.entity.Department;
import com.example.studentAttendanceSystemProject.repository.DepartmentRepository;

@Service
public class DeprtmentServiceImpl implements DepartmentService{

	
	 @Autowired
	    private DepartmentRepository repo;
	    
	@Override
	public void save(Department dept) {
		// TODO Auto-generated method stub
		 repo.save(dept);
	}

	@Override
	public List<Department> getAll() {
		// TODO Auto-generated method stub
		return repo.findAll();
	}

	@Override
	public Department getById(Long id) {
		// TODO Auto-generated method stub
		return repo.findById(id).orElse(null);
	}

	@Override
	public void delete(Long id) {
		// TODO Auto-generated method stub
		 repo.deleteById(id);
	}

	@Override
	public List<Department> getAllDepartments() {
		// TODO Auto-generated method stub
		return repo.findAll();
	}

}
