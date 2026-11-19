package com.example.studentAttendanceSystemProject.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;

    private String year;
    private String division;

    @ManyToOne
    private Subject subject;

    @ManyToOne
    private Teacher teacher;

    @ManyToOne
    private PreVerifiedStudent pstudent;

    @ManyToOne
    @JoinColumn(name = "lecture_session_id")
    private LectureSession lectureSession;

    private boolean present; // true = present, false = absent

    private String markedVia = "MANUAL_TEACHER"; // "QR_SCAN" or "MANUAL_TEACHER"

    private LocalDateTime scanTimestamp;

    private String studentMobile;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(String division) {
        this.division = division;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    public PreVerifiedStudent getPstudent() {
        return pstudent;
    }

    public void setPstudent(PreVerifiedStudent pstudent) {
        this.pstudent = pstudent;
    }

    public LectureSession getLectureSession() {
        return lectureSession;
    }

    public void setLectureSession(LectureSession lectureSession) {
        this.lectureSession = lectureSession;
    }

    public boolean isPresent() {
        return present;
    }

    public void setPresent(boolean present) {
        this.present = present;
    }

    public String getMarkedVia() {
        return markedVia;
    }

    public void setMarkedVia(String markedVia) {
        this.markedVia = markedVia;
    }

    public LocalDateTime getScanTimestamp() {
        return scanTimestamp;
    }

    public void setScanTimestamp(LocalDateTime scanTimestamp) {
        this.scanTimestamp = scanTimestamp;
    }

    public String getStudentMobile() {
        return studentMobile;
    }

    public void setStudentMobile(String studentMobile) {
        this.studentMobile = studentMobile;
    }
}
