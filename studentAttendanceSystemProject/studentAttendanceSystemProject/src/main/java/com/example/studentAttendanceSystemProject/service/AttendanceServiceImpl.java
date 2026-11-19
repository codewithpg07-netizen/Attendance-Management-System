package com.example.studentAttendanceSystemProject.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.studentAttendanceSystemProject.entity.Attendance;
import com.example.studentAttendanceSystemProject.entity.PreVerifiedStudent;
import com.example.studentAttendanceSystemProject.entity.Subject;
import com.example.studentAttendanceSystemProject.repository.AttendanceRepository;


@Service
public class AttendanceServiceImpl implements AttendanceService{

	@Autowired
	AttendanceRepository attendanceRepository;
	@Override
	public List<Attendance> getByStudentAndSubject(PreVerifiedStudent student, Subject subject) {
		return attendanceRepository.findByPstudentAndSubjectOrderByDateAsc(student, subject);
	}
//	@Override
//	public Attendance saveOrUpdateAttendance(Attendance attendance) {
//		
//		
//		Attendance existing = attendanceRepository
//	                .findByPstudentIdAndSubjectIdAndDateAndTeacherId(
//	                        attendance.getPstudent().getId(),
//	                        attendance.getSubject().getId(),
//	                        attendance.getDate(),
//	                        attendance.getTeacher().getId()
//	                );
//		 
//		  if(existing != null){
//	            existing.setPresent(attendance.isPresent()); // update present status
//	            return attendanceRepository.save(existing);
//	        }
//
//	        return attendanceRepository.save(attendance);
//	}
//	
	
	@Override
	public Attendance saveOrUpdateAttendance(Attendance attendance) {

	    // Attendance.date is already LocalDate, no need to convert
	    LocalDate onlyDate = attendance.getDate(); // use as is

	    // check if attendance already exists
	    Attendance existing = attendanceRepository
	        .findByPstudentIdAndSubjectIdAndDateAndTeacherId(
	            attendance.getPstudent().getId(),
	            attendance.getSubject().getId(),
	            onlyDate,
	            attendance.getTeacher().getId()
	        );

	    if (existing != null) {
	        // update existing record
	        existing.setPresent(attendance.isPresent());
	        return attendanceRepository.save(existing);
	    }

	    // insert new record
	    attendance.setDate(onlyDate);
	    return attendanceRepository.save(attendance);
	}


}
