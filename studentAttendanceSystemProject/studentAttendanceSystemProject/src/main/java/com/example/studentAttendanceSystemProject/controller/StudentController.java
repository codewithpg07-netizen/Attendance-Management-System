package com.example.studentAttendanceSystemProject.controller;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.studentAttendanceSystemProject.entity.Attendance;
import com.example.studentAttendanceSystemProject.entity.PreVerifiedStudent;
import com.example.studentAttendanceSystemProject.entity.Subject;
import com.example.studentAttendanceSystemProject.service.AttendanceService;
import com.example.studentAttendanceSystemProject.service.StudentService;
import com.example.studentAttendanceSystemProject.service.SubjectService;

import jakarta.servlet.http.HttpServletResponse;
//Excel workbook types
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

@Controller
@RequestMapping("/student")
public class StudentController {
	
	
	 @Autowired
	    private StudentService studentService;
	 
	 
	 @Autowired
	    private SubjectService subjectService;
	 
	 @Autowired
	    private AttendanceService attendanceService;

	 
	  @GetMapping("/dashboard")
	    public String dashboard(Model model, Principal principal) {

	        // 1️⃣ Logged-in student
	        PreVerifiedStudent student = studentService.getStudentByEmail(principal.getName());

	        // 2️⃣ Subjects of student
	        List<Subject> subjects = subjectService.findByDepartmentAndYear(
	                student.getDepartment(),
	                student.getYear()
	        );

	        // 3️⃣ Attendance summary
	        List<Map<String, Object>> attendanceList = new ArrayList<>();

	        for (Subject sub : subjects) {
	            Map<String, Object> map = new HashMap<>();
	            map.put("subject", sub);
	            
	            // Calculate attendance % for this student and subject
	            double percent = studentService.calculateAttendance(student, sub);
	            map.put("attendancePercent", percent);
	            map.put("attendancePercentFormatted", String.format("%.1f%%", percent));
	            map.put("lowAttendance", percent < 75);

	            attendanceList.add(map);
	        }

	        // 4️⃣ Add attributes to model
	        model.addAttribute("student", student);
	        model.addAttribute("subjects", subjects);
	        model.addAttribute("attendanceList", attendanceList);

	        return "student/dashboard";
	    }
	  
	  @GetMapping("/subject/{subjectId}/attendance")
	  public String subjectAttendanceDetail(@PathVariable Long subjectId, Model model, Principal principal) {
	      PreVerifiedStudent student = studentService.getStudentByEmail(principal.getName());
	      Subject subject = subjectService.getById(subjectId);

	      // Student ki daily attendance for selected subject
	      List<Attendance> dailyAttendance = attendanceService.getByStudentAndSubject(student, subject);

	      model.addAttribute("student", student);
	      model.addAttribute("subject", subject);
	      model.addAttribute("dailyAttendance", dailyAttendance);

	      return "student/subject-attendance";
	  }
	  
	  @GetMapping("/attendance/download/excel/{subjectId}")
	  public void downloadAttendanceExcel(@PathVariable Long subjectId,
	                                      HttpServletResponse response,
	                                      Principal principal) throws IOException {

	      PreVerifiedStudent student = studentService.getStudentByEmail(principal.getName());
	      Subject subject = subjectService.getById(subjectId);

	      List<Attendance> attendances = studentService.getAttendanceByStudentAndSubject(student, subject);

	      response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	      response.setHeader("Content-Disposition", "attachment; filename=attendance_" + subject.getSubjectName() + ".xlsx");

	      Workbook workbook = new XSSFWorkbook();
	      Sheet sheet = workbook.createSheet("Attendance");

	      Row header = sheet.createRow(0);
	      header.createCell(0).setCellValue("Date");
	      header.createCell(1).setCellValue("Present");

	      int rowIdx = 1;
	      for (Attendance att : attendances) {
	          Row row = sheet.createRow(rowIdx++);
	          row.createCell(0).setCellValue(att.getDate().toString());
	          row.createCell(1).setCellValue(att.isPresent() ? "Present" : "Absent");
	      }

	      workbook.write(response.getOutputStream());
	      workbook.close();
	  }

	  @GetMapping("/attendance/{subjectId}")
	  public String attendanceDetail(@PathVariable Long subjectId, Model model, Principal principal) {
	      PreVerifiedStudent student = studentService.getStudentByEmail(principal.getName());
	      Subject subject = subjectService.getById(subjectId);

	      List<Attendance> attendanceList = studentService.getAttendanceByStudentAndSubject(student, subject);

	      model.addAttribute("student", student);
	      model.addAttribute("subject", subject);
	      model.addAttribute("attendanceList", attendanceList);

	      return "student/attendance-detail";
	  }
	



}
