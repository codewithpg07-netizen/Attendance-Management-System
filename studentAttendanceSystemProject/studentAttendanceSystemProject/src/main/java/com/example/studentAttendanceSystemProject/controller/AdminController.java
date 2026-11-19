package com.example.studentAttendanceSystemProject.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.example.studentAttendanceSystemProject.entity.Department;
import com.example.studentAttendanceSystemProject.entity.PreVerifiedStudent;
import com.example.studentAttendanceSystemProject.entity.Subject;
import com.example.studentAttendanceSystemProject.entity.Teacher;
import com.example.studentAttendanceSystemProject.entity.User;
import com.example.studentAttendanceSystemProject.repository.DepartmentRepository;
import com.example.studentAttendanceSystemProject.repository.PreVerifiedStudentRepository;
import com.example.studentAttendanceSystemProject.repository.UserRepository;
import com.example.studentAttendanceSystemProject.service.AdminService;
import com.example.studentAttendanceSystemProject.service.DepartmentService;
import com.example.studentAttendanceSystemProject.service.SubjectService;
import com.example.studentAttendanceSystemProject.service.TeacherService;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;


@Controller
@RequestMapping("/admin")
public class AdminController {
	 @Autowired
	    private UserRepository userRepository;
	
	 @Autowired
	    private AdminService adminService;
	 @Autowired
	 private PreVerifiedStudentRepository preVerifiedStudentRepository;
	 
	 @Autowired
	    private TeacherService teacherService;
	 
	 @Autowired
	 DepartmentRepository departmentRepository;

	 @Autowired
	 private DepartmentService departmentService;
	 
	 @Autowired
	 private SubjectService subjectService;
	 
	 @Autowired
	 private PasswordEncoder passwordEncoder;

	 @GetMapping("/dashboard")
	 public String dashboard(
	         @RequestParam(required = false) Long departmentId,
	         @RequestParam(required = false) String year,
	         Model model) {

	     List<PreVerifiedStudent> students;

	     if (departmentId != null && year != null && !year.isEmpty()) {
	         students = preVerifiedStudentRepository
	                 .findByDepartment_IdAndYear(departmentId, year);

	     } else if (departmentId != null) {
	         students = preVerifiedStudentRepository
	                 .findByDepartment_Id(departmentId);

	     } else if (year != null && !year.isEmpty()) {
	         students = preVerifiedStudentRepository
	                 .findByYear(year);

	     } else {
	         students = preVerifiedStudentRepository.findAll();
	     }

	     model.addAttribute("students", students);
	     model.addAttribute("teachers", adminService.getAllTeachers());
	     model.addAttribute("departments", departmentService.getAll());

	     return "admin/dashboard";
	 }

	    @GetMapping("/students")
	    public String studentList(Model model) {
	        List<User> students = userRepository.findByRole("STUDENT");
	        model.addAttribute("students", students);
	        return "admin/student_list";
	    }
	    
	   
	    @PostMapping("/upload-students")
	    public String uploadStudents(@RequestParam("file") MultipartFile file, Model model) throws Exception {
	    	 try {
	             adminService.uploadStudentList(file);
	             model.addAttribute("success", "Students uploaded successfully!");
	         } catch (Exception e) {
	             model.addAttribute("error", "Error uploading file: " + e.getMessage());
	         }
//	         return "admin/view-students";
	    	 return "redirect:/admin/view-students";
	     }


	  


	    
   
	  
	    
	    @GetMapping("/view-students")
	    public String viewStudents(
	            @RequestParam(required = false) Long departmentId,
	            @RequestParam(required = false) String year,
	            Model model) {

	        // 🔹 Call service method which handles filtering + duplicate removal
	        List<PreVerifiedStudent> students = adminService.getFilteredStudents(departmentId, year);

	        model.addAttribute("students", students);
	        model.addAttribute("departments", departmentService.getAllDepartments());

	        return "admin/view-students";
	    }

	    
	    // Delete student
	    @GetMapping("/delete-student/{id}")
	    public String deleteStudent(@PathVariable Long id) {
	        adminService.deleteStudent(id);
	        return "redirect:/admin/view-students";
	    }


	    @GetMapping("/edit-student/{id}")
	    public String editStudent(@PathVariable Long id, Model model) {

	        PreVerifiedStudent student =
	                preVerifiedStudentRepository.findById(id).orElseThrow();

	        List<Department> departments = departmentRepository.findAll();

	        model.addAttribute("student", student);
	        model.addAttribute("departments", departments);

	        return "admin/edit-student";
	    }

	    @PostMapping("/update-student")
	    public String updateStudent(
	            @RequestParam Long id,
	            @RequestParam String rollNumber,
	            @RequestParam String name,
	            @RequestParam String email,
	            @RequestParam Long departmentId,
	            @RequestParam String year,
	            @RequestParam String section) {

	        PreVerifiedStudent student =
	                preVerifiedStudentRepository.findById(id).orElseThrow();

	        Department dept =
	                departmentRepository.findById(departmentId).orElseThrow();

	        student.setRollNumber(rollNumber);
	        student.setName(name);
	        student.setEmail(email);
	        student.setDepartment(dept);   // 🔥 IMPORTANT
	        student.setYear(year);
	        student.setSection(section);

	        preVerifiedStudentRepository.save(student);

	        return "redirect:/admin/view-students";
	    }


	    
	    // View all teachers
	    @GetMapping("/view-teachers")
	    public String viewTeachers(Model model) {
	        model.addAttribute("teachers", teacherService.getAllTeachers());
	        return "admin/view-teachers";
	    }

	    // Show add teacher form
	    @GetMapping("/add-teacher")
	    public String addTeacherForm(Model model) {
	    	  model.addAttribute("teacher", new Teacher());
	    	  model.addAttribute("departments", departmentService.getAll());
	    	    model.addAttribute("subjects", subjectService.getAll());
	    	  return "admin/add-teacher";
	    }
	    
	

	    
	    @PostMapping("/add-teachers")
	    public String addTeacher(@ModelAttribute Teacher teacher, Model model) {

	        if (teacher.getPassword() == null || teacher.getPassword().trim().isEmpty()) {
	            model.addAttribute("error", "Password cannot be empty!");
	            model.addAttribute("departments", departmentService.getAll());
	            return "admin/add-teacher";
	        }

	        // fetch full department object
	        Department dept = departmentService.getById(teacher.getDepartment().getId());
	        teacher.setDepartment(dept);

	        // Initially, teacher has no subject
	        teacher.setSubject(null);

	        teacherService.addTeacher(teacher);

	        // Add teacher to User table
	        User teacherUser = new User();
	        teacherUser.setName(teacher.getName());
	        teacherUser.setEmail(teacher.getEmail());
	        teacherUser.setPassword(teacher.getPassword()); // already encoded in service
	        teacherUser.setRole("ROLE_TEACHER");
	        userRepository.save(teacherUser);

	        return "redirect:/admin/view-teachers";
	    }

	 
	    // EDIT FORM
	    @GetMapping("/edit-teacher/{id}")
	    public String editTeacher(@PathVariable Long id, Model model) {

	        Teacher teacher = teacherService.getTeacherById(id);

	        model.addAttribute("teacher", teacher);
	        model.addAttribute("departments", departmentService.getAll());
	        model.addAttribute("subjects", subjectService.getAll());

	        return "admin/edit-teacher";
	    }


	    // UPDATE TEACHER
	    @PostMapping("/update-teacher")
	    public String updateTeacher(@ModelAttribute Teacher teacher) {

	        Department dept =
	            departmentService.getById(teacher.getDepartment().getId());

	        Subject sub =
	            subjectService.getById(teacher.getSubject().getId());

	        teacher.setDepartment(dept);
	        teacher.setSubject(sub);

	        teacherService.updateTeacher(teacher);
	        return "redirect:/admin/view-teachers";
	    }

	    
	    @GetMapping("/delete-teacher/{id}")
	    public String deleteTeacher(@PathVariable Long id) {
	        teacherService.deleteTeacher(id);
	        return "redirect:/admin/view-teachers";
	    }
	    
	    
}
