package com.example.studentAttendanceSystemProject.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.studentAttendanceSystemProject.entity.Department;
import com.example.studentAttendanceSystemProject.entity.PreVerifiedStudent;
import com.example.studentAttendanceSystemProject.entity.User;
import com.example.studentAttendanceSystemProject.repository.DepartmentRepository;
import com.example.studentAttendanceSystemProject.repository.PreVerifiedStudentRepository;
import com.example.studentAttendanceSystemProject.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
 
public class AdminServiceImpl implements AdminService {

	
	  @Autowired
	    private UserRepository userRepository;
	  @Autowired
	    private PreVerifiedStudentRepository preVerifiedStudentRepository;

	    @Autowired
	    private PasswordEncoder passwordEncoder;
	 @Autowired
	 DepartmentRepository departmentRepository;
	  
	

	@Override
	public void addTeacher(User teacher) {
		// TODO Auto-generated method stub
		teacher.setRole("TEACHER");
        teacher.setPassword(passwordEncoder.encode(teacher.getPassword()));
        userRepository.save(teacher);
	}

	@Override
	public List<User> getAllTeachers() {
		// TODO Auto-generated method stub
		 return userRepository.findAll().stream()
	                .filter(u -> "TEACHER".equals(u.getRole()))
	                .toList();
	}

	@Override
	public List<PreVerifiedStudent> getAllStudents() {
		// TODO Auto-generated method stub
		return preVerifiedStudentRepository.findAll();
	}


	

	
	public void uploadStudentList(MultipartFile file) throws Exception {
	    if (file.isEmpty()) {
	        System.out.println("File is empty!");
	        return;
	    }

	    try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
	        String line;
	        boolean firstLine = true;

	        while ((line = reader.readLine()) != null) {
	            if (firstLine) {
	                firstLine = false; // skip header
	                continue;
	            }

	            String[] data = line.split(",");
	            if (data.length < 6) {
	                System.out.println("Skipping invalid row: " + line);
	                continue;
	            }

	            String deptName = data[3].trim();
	            Department dept = departmentRepository.findByName(deptName);
	            if (dept == null) {
	                System.out.println("Department not found: " + deptName);
	                continue;
	            }

	            String rollNumber = data[0].trim();
	            // 🔹 Only accept roll numbers starting with "ROLL"
	            if (!rollNumber.toUpperCase().startsWith("ROLL")) {
	                System.out.println("Skipping row due to invalid roll number format: " + line);
	                continue; // skip this row
	            }
	            String email = data[2].trim();

	            // 🔹 Check if student already exists by rollNumber or email
	            PreVerifiedStudent existingStudent = preVerifiedStudentRepository
	                    .findByRollNumberOrEmail(rollNumber, email);

	            if (existingStudent != null) {
	                // Update existing student info
	                existingStudent.setName(data[1].trim());
	                existingStudent.setDepartment(dept);
	                existingStudent.setYear(data[4].trim());
	                existingStudent.setSection(data[5].trim());
	                existingStudent.setRegistered(false);
	                preVerifiedStudentRepository.save(existingStudent);
	                continue;
	            }

	            // Insert new student
	            PreVerifiedStudent s = new PreVerifiedStudent();
	            s.setRollNumber(rollNumber);
	            s.setName(data[1].trim());
	            s.setEmail(email);
	            s.setDepartment(dept);
	            s.setYear(data[4].trim());
	            s.setSection(data[5].trim());
	            s.setRegistered(false);

	            preVerifiedStudentRepository.save(s);
	        }
	    }
	}

	
//	public void uploadStudentList(MultipartFile file) throws Exception {
//
//	    if (file.isEmpty()) {
//	        System.out.println("File is empty!");
//	        return;
//	    }
//
//	    try (BufferedReader reader = new BufferedReader(
//	            new InputStreamReader(file.getInputStream()))) {
//
//	        String line;
//	        boolean firstLine = true;
//
//	        while ((line = reader.readLine()) != null) {
//
//	            if (firstLine) {
//	                firstLine = false;
//	                continue;
//	            }
//
//	            String[] data = line.split(",");
//	            if (data.length < 6) {
//	                System.out.println("Skipping invalid row: " + line);
//	                continue;
//	            }
//
//	            String deptName = data[3].trim();
//	            Department dept = departmentRepository.findByName(deptName);
//
//	            if (dept == null) {
//	                System.out.println("Department not found: " + deptName);
//	                continue;
//	            }
//
//	            PreVerifiedStudent s = new PreVerifiedStudent();
//	            s.setRollNumber(data[0].trim());
//	            s.setName(data[1].trim());
//	            s.setEmail(data[2].trim());
//	            s.setDepartment(dept); // 🔥 KEY CHANGE
//	            s.setYear(data[4].trim());
//	            s.setSection(data[5].trim());
//	            s.setRegistered(false);
//
//	            preVerifiedStudentRepository.save(s);
//	        }
//	    }
//	}


	@Override
	public PreVerifiedStudent getStudentById(Long id) {
		return preVerifiedStudentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
	}

	@Override
	public void deleteStudent(Long id) {
		// TODO Auto-generated method stub
		 preVerifiedStudentRepository.deleteById(id);
	}

	@Override
	public PreVerifiedStudent saveStudent(PreVerifiedStudent student) {
		// TODO Auto-generated method stub
		  return preVerifiedStudentRepository.save(student); // save or update
	}

	@Override
	public List<PreVerifiedStudent> getFilteredStudents(Long departmentId, String year) {
	    List<PreVerifiedStudent> filteredStudents;

	    if (departmentId != null && year != null && !year.isEmpty()) {
	        filteredStudents = preVerifiedStudentRepository.findByDepartmentIdAndYear(departmentId, year);
	    } else if (departmentId != null) {
	        filteredStudents = preVerifiedStudentRepository.findByDepartmentId(departmentId);
	    } else if (year != null && !year.isEmpty()) {
	        filteredStudents = preVerifiedStudentRepository.findByYear(year);
	    } else {
	        filteredStudents = preVerifiedStudentRepository.findAll();
	    }

	    // Remove duplicates by Email
	    Map<String, PreVerifiedStudent> uniqueMap = new LinkedHashMap<>();
	    for (PreVerifiedStudent student : filteredStudents) {
	        uniqueMap.put(student.getEmail(), student); // email as unique key
	    }

	    return new ArrayList<>(uniqueMap.values());
	}

}
