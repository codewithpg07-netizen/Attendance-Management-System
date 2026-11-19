package com.example.studentAttendanceSystemProject.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.studentAttendanceSystemProject.entity.Department;
import com.example.studentAttendanceSystemProject.entity.PreVerifiedStudent;


@Repository
public interface PreVerifiedStudentRepository extends JpaRepository<PreVerifiedStudent, Long> {

	 PreVerifiedStudent findByEmailAndRollNumber(String email, String rollNumber);

	List<PreVerifiedStudent> findAll();

	List<PreVerifiedStudent> findByDepartmentId(Long id);

	PreVerifiedStudent findByEmail(String email);

	List<PreVerifiedStudent> findByDepartmentName(String name);

	List<PreVerifiedStudent> findByDepartmentNameAndYear(String department, String year);

	List<PreVerifiedStudent> findByYear(String year);



	List<PreVerifiedStudent> findByDepartment_Id(Long departmentId);

	List<PreVerifiedStudent> findByDepartment_IdAndYear(Long departmentId, String year);

	List<PreVerifiedStudent> findByDepartmentIdAndYear(Long departmentId, String year);

	List<PreVerifiedStudent> findByDepartmentAndYear(Department department, String year);


	PreVerifiedStudent findByRollNumberOrEmail(String rollNumber, String email);

}
