package com.example.studentAttendanceSystemProject.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Student {
	
	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    private String rollNumber;
	    private String name;
	    @Column(unique = true)
	    private String email;

//	    private String department;
	    @ManyToOne
	    @JoinColumn(name = "department_id")
	    private Department department;

	    private String year;
	    private String section;
	    private String division;
	    private String mobileNumber;
	    private String parentMobileNumber;
		public Long getId() {
			return id;
		}
		public void setId(Long id) {
			this.id = id;
		}
		public String getRollNumber() {
			return rollNumber;
		}
		public void setRollNumber(String rollNumber) {
			this.rollNumber = rollNumber;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getEmail() {
			return email;
		}
		public void setEmail(String email) {
			this.email = email;
		}
		public String getYear() {
			return year;
		}
		public void setYear(String year) {
			this.year = year;
		}
		public String getSection() {
			return section;
		}
		public void setSection(String section) {
			this.section = section;
		}
		public String getDivision() {
			return division != null && !division.isEmpty() ? division : section;
		}
		public void setDivision(String division) {
			this.division = division;
		}
		public String getMobileNumber() {
			return mobileNumber;
		}
		public void setMobileNumber(String mobileNumber) {
			this.mobileNumber = mobileNumber;
		}
		public String getParentMobileNumber() {
			return parentMobileNumber;
		}
		public void setParentMobileNumber(String parentMobileNumber) {
			this.parentMobileNumber = parentMobileNumber;
		}
		public Department getDepartment() {
			return department;
		}
		public void setDepartment(Department department) {
			this.department = department;
		}
	    
	    

}
