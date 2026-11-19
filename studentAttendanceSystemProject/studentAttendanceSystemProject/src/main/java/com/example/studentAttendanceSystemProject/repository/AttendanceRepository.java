package com.example.studentAttendanceSystemProject.repository;

import java.time.LocalDate;
import java.util.List;
import com.example.studentAttendanceSystemProject.entity.Subject;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.studentAttendanceSystemProject.entity.Attendance;
import com.example.studentAttendanceSystemProject.entity.PreVerifiedStudent;
import com.example.studentAttendanceSystemProject.entity.Teacher;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance,Long>{

//	
//	 // Total classes for a subject
//    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.subject = :subject")
//    long countBySubject(@Param("subject") Subject subject);
//    
//    
//
//    // Total classes present for a student in a subject
//    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.subject = :subject AND a.pstudent = :student AND a.present = true")
//    long countByStudentAndSubject(@Param("student") PreVerifiedStudent student, @Param("subject") Subject subject);
//    
//    
//    // Optional: list all students with their attendance for a subject
//    List<Attendance> findBySubject(Subject subject);
//    
//    
//	 List<Attendance> findByTeacherAndSubjectAndDate(Teacher teacher, Subject subject, LocalDate date);
//	    List<Attendance> findByTeacherAndDate(Teacher teacher, LocalDate date);
//	    List<Attendance> findByTeacher(Teacher teacher);
//		List<Attendance> findByTeacherAndSubjectAndPstudent(Teacher teacher, Subject subject,
//				PreVerifiedStudent student);
//
//
//
//		long countBySubject1(Subject subject);
//		
		
		// Total classes for a subject
	    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.subject = :subject")
	    long countBySubject(@Param("subject") Subject subject);

	    // Total classes present for a student in a subject
	    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.subject = :subject AND a.pstudent = :student AND a.present = true")
	    long countByStudentAndSubject(@Param("student") PreVerifiedStudent student, @Param("subject") Subject subject);

	    // Optional: list all students with their attendance for a subject
	    List<Attendance> findBySubject(Subject subject);

	    // Attendance for a teacher, subject, and date
	    List<Attendance> findByTeacherAndSubjectAndDate(Teacher teacher, Subject subject, LocalDate date);

	    // Attendance for a teacher on a specific date
	    List<Attendance> findByTeacherAndDate(Teacher teacher, LocalDate date);

	    // All attendance records for a teacher
	    List<Attendance> findByTeacher(Teacher teacher);

	    // Attendance for a specific student, teacher, and subject
	    List<Attendance> findByTeacherAndSubjectAndPstudent(Teacher teacher, Subject subject, PreVerifiedStudent student);

		List<Attendance> findByPstudentAndSubjectOrderByDateAsc(PreVerifiedStudent student, Subject subject);




//		Attendance findByPstudentIdAndSubjectIdAndDateAndTeacherId(
//		        Long pstudentId, Long subjectId, String date, Long teacherId);

		Attendance findByPstudentIdAndSubjectIdAndDateAndTeacherId(Long id, Long id2, LocalDate date, Long id3);


}
