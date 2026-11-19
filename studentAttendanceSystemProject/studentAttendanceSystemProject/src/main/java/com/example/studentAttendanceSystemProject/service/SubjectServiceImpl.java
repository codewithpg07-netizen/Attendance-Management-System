package com.example.studentAttendanceSystemProject.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.studentAttendanceSystemProject.entity.Department;
import com.example.studentAttendanceSystemProject.entity.Subject;
import com.example.studentAttendanceSystemProject.entity.Teacher;
import com.example.studentAttendanceSystemProject.repository.SubjectRepository;

@Service
public class SubjectServiceImpl implements SubjectService{

	
	  @Autowired
	    private SubjectRepository repo;
	  
	  @Autowired
	    private SubjectRepository subjectRepository;
	  
	@Override
	public void save(Subject subject) {
		// TODO Auto-generated method stub
		repo.save(subject);
	}

	@Override
	public List<Subject> getAll() {
		// TODO Auto-generated method stub
		return repo.findAll();
	}

	@Override
	public void delete(Long id) {
		// TODO Auto-generated method stub
		 repo.deleteById(id);
	}

	@Override
	public Subject getById(Long id) {
		// TODO Auto-generated method stub
		return repo.findById(id).orElse(null);
	}

	@Override
	public List<Subject> getByTeacher(Teacher teacher) {
		return subjectRepository.findByTeacher(teacher);
	}

	@Override
	public List<Subject> findByTeacher(Teacher teacher) {
		// TODO Auto-generated method stub
		return  subjectRepository.findByTeacher(teacher);
	}

	@Override
	public List<Subject> findByDepartmentAndYear(Department department, String year) {
		return subjectRepository.findByDepartmentAndYear(department, year);
	}

	
	
}
