package com.example.studentAttendanceSystemProject.service;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.studentAttendanceSystemProject.entity.Attendance;
import com.example.studentAttendanceSystemProject.entity.PreVerifiedStudent;
import com.example.studentAttendanceSystemProject.entity.Subject;
import com.example.studentAttendanceSystemProject.repository.AttendanceRepository;
import com.example.studentAttendanceSystemProject.repository.PreVerifiedStudentRepository;
import com.example.studentAttendanceSystemProject.repository.SubjectRepository;
import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfDocument;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Workbook;
//

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class StudentServiceImpl implements StudentService {

	
	@Autowired
    private PreVerifiedStudentRepository studentRepo;

    @Autowired
    private SubjectRepository subjectRepo;
    
    @Autowired
    private AttendanceRepository attendanceRepository;
	  

		@Override
		public PreVerifiedStudent getStudentByEmail(String email) {
			// TODO Auto-generated method stub
			return  studentRepo.findByEmail(email);
		}
//
//		@Override
//		public List<Subject> getSubjectsForStudent(PreVerifiedStudent student) {
//			// TODO Auto-generated method stub
//			return subjectRepo.findByDepartmentName(student.getDepartment());
//		}


		@Override
		public List<Map<String, Object>> getAttendanceSummary(PreVerifiedStudent student) {
			 List<Subject> subjects =
		                subjectRepo.findByDepartmentAndYear(
		                        student.getDepartment(),
		                        student.getYear()
		                );

		        List<Map<String, Object>> list = new ArrayList<>();

		        for (Subject subject : subjects) {

		            long total =
		                    attendanceRepository.countBySubject(subject);

		            long present =
		                    attendanceRepository.countByStudentAndSubject(
		                            student, subject);
		            
		            double percent = total > 0
		                    ? (present * 100.0) / total
		                    : 0;

		            Map<String, Object> map = new HashMap<>();
		            map.put("subject", subject);
		            map.put("total", total);
		            map.put("present", present);
		            map.put("absent", total - present);
		            map.put("percent", percent);

		            list.add(map);
		        }
		        return list;
		}



	    // Calculate attendance % for a student and a subject
		@Override
		public double calculateAttendance(PreVerifiedStudent student, Subject subject) {
			 long totalClasses = attendanceRepository.countBySubject(subject);
		        if (totalClasses == 0) return 0.0;

		        long presentCount = attendanceRepository.countByStudentAndSubject(student, subject);
		        return (presentCount * 100.0) / totalClasses;
		}


		@Override
		public List<Attendance> getAttendanceByStudentAndSubject(PreVerifiedStudent student, Subject subject) {
			return attendanceRepository.findByPstudentAndSubjectOrderByDateAsc(student, subject);
		}


		@Override
		public void generateExcel(PreVerifiedStudent student, Subject subject, HttpServletResponse response) {
			// TODO Auto-generated method stub
			try (Workbook workbook = new XSSFWorkbook()) {
	            Sheet sheet = workbook.createSheet("Attendance");

	            // Header
	            Row header = sheet.createRow(0);
	            header.createCell(0).setCellValue("Date");
	            header.createCell(1).setCellValue("Status");

	            // Fetch attendance
	            List<Attendance> attendanceList = getAttendanceByStudentAndSubject(student, subject);

	            int rowNum = 1;
	            for (Attendance att : attendanceList) {
	                Row row = sheet.createRow(rowNum++);
	                row.createCell(0).setCellValue(att.getDate().toString());
	                row.createCell(1).setCellValue(att.isPresent() ? "Present" : "Absent");
	            }

	            // Auto-size columns
	            sheet.autoSizeColumn(0);
	            sheet.autoSizeColumn(1);

	            // Response
	            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	            String fileName = student.getName() + "_" + subject.getSubjectName() + "_Attendance.xlsx";
	            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

	            workbook.write(response.getOutputStream());
	            response.getOutputStream().flush();

	        } catch (IOException e) {
	            e.printStackTrace();
	        }
		}


//		@Override
//		public List<PreVerifiedStudent> getFilteredStudents(Long departmentId, String year) {
//			// TODO Auto-generated method stub
//			
//			  // 1️⃣ Get filtered students from DB
//		    List<PreVerifiedStudent> filteredStudents = studentRepo.findByDepartmentIdAndYear(departmentId, year);
//
//		    // 2️⃣ Remove duplicates by Email
//		    Map<String, PreVerifiedStudent> uniqueMap = new LinkedHashMap<>();
//		    for (PreVerifiedStudent student : filteredStudents) {
//		        uniqueMap.put(student.getEmail(), student); // email as unique key
//		    }
//		    
//			return new ArrayList<>(uniqueMap.values());
//		}}


		
}


