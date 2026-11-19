package com.example.studentAttendanceSystemProject.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class SmsAlertLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private PreVerifiedStudent student;

    private String parentMobile;

    private String monthYear; // e.g., "November 2026"

    private double attendancePercentage;

    @Column(length = 2000)
    private String messageContent;

    // SENT, SIMULATED, FAILED
    private String status = "SENT";

    private LocalDateTime sentAt = LocalDateTime.now();

    public SmsAlertLog() {
    }

    public SmsAlertLog(PreVerifiedStudent student, String parentMobile, String monthYear,
                       double attendancePercentage, String messageContent, String status) {
        this.student = student;
        this.parentMobile = parentMobile;
        this.monthYear = monthYear;
        this.attendancePercentage = attendancePercentage;
        this.messageContent = messageContent;
        this.status = status;
        this.sentAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PreVerifiedStudent getStudent() {
        return student;
    }

    public void setStudent(PreVerifiedStudent student) {
        this.student = student;
    }

    public String getParentMobile() {
        return parentMobile;
    }

    public void setParentMobile(String parentMobile) {
        this.parentMobile = parentMobile;
    }

    public String getMonthYear() {
        return monthYear;
    }

    public void setMonthYear(String monthYear) {
        this.monthYear = monthYear;
    }

    public double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
}
