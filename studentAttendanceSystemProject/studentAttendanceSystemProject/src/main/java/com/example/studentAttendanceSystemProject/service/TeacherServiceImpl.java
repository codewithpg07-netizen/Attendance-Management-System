package com.example.studentAttendanceSystemProject.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.studentAttendanceSystemProject.entity.Teacher;
import com.example.studentAttendanceSystemProject.repository.TeacherRepository;

@Service
public class TeacherServiceImpl implements TeacherService {

	
	@Autowired
	private TeacherRepository teacherRepository;
	
	 @Autowired
	    private PasswordEncoder passwordEncoder;
	 
	 

	@Override
	public void addTeacher(Teacher teacher) {
		// TODO Auto-generated method stub
		 if(teacher.getPassword() == null || ((String) teacher.getPassword()).trim().isEmpty()) {
		        throw new IllegalArgumentException("Password cannot be null or empty");
		    }
		 
		 teacher.setPassword(passwordEncoder.encode(teacher.getPassword()));
	        teacherRepository.save(teacher);
	}


	@Override
	public List<Teacher> getAllTeachers() {
		// TODO Auto-generated method stub
		return teacherRepository.findAll();
	}


	@Override
	public Teacher getTeacherById(Long id) {
		 return teacherRepository.findById(id)
	                .orElseThrow(() -> new RuntimeException("Teacher not found"));
	}


	@Override
	public Teacher updateTeacher(Teacher teacher) {
		// TODO Auto-generated method stub
		return  teacherRepository.save(teacher);
	}


	@Override
	public void deleteTeacher(Long id) {
		// TODO Auto-generated method stub
		teacherRepository.deleteById(id);
	}

	@Override
	public Teacher getByEmail(String email) {
	    return teacherRepository.findByEmail(email)
	            .orElseThrow(() ->
	                new RuntimeException("Teacher not found with email: " + email));
	}



	@Override
	public Teacher getById(Long id) {
		// TODO Auto-generated method stub
		return teacherRepository.findById(id).orElse(null);
	}






}
