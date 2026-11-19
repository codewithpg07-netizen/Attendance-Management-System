package com.example.studentAttendanceSystemProject.service;

import java.util.List;
import java.util.Map;

import com.example.studentAttendanceSystemProject.entity.Attendance;
import com.example.studentAttendanceSystemProject.entity.PreVerifiedStudent;
import com.example.studentAttendanceSystemProject.entity.Subject;

import jakarta.servlet.http.HttpServletResponse;

public interface StudentService {

	PreVerifiedStudent getStudentByEmail(String name);

	List<Map<String, Object>> getAttendanceSummary(PreVerifiedStudent student);

	public double calculateAttendance(PreVerifiedStudent student, Subject sub);

	List<Attendance> getAttendanceByStudentAndSubject(PreVerifiedStudent student, Subject subject);

	void generateExcel(PreVerifiedStudent student, Subject subject, HttpServletResponse response);

//	public List<PreVerifiedStudent> getFilteredStudents(Long departmentId, String year);
}
