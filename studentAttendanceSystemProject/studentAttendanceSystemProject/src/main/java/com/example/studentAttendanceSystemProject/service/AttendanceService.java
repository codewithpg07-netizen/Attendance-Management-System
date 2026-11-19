package com.example.studentAttendanceSystemProject.service;

import java.util.List;

import com.example.studentAttendanceSystemProject.entity.Attendance;
import com.example.studentAttendanceSystemProject.entity.PreVerifiedStudent;
import com.example.studentAttendanceSystemProject.entity.Subject;

public interface AttendanceService {

	List<Attendance> getByStudentAndSubject(PreVerifiedStudent student, Subject subject);

	
	 Attendance saveOrUpdateAttendance(Attendance attendance);
}
