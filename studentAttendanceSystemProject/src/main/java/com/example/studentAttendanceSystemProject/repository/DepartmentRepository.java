package com.example.studentAttendanceSystemProject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.studentAttendanceSystemProject.entity.Department;

@Repository
public interface DepartmentRepository extends JpaRepository<Department,Long>{

	Department findByName(String deptName);

}
