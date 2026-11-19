package com.example.studentAttendanceSystemProject.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class AttendanceSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String collegeName = "Government Polytechnic / Engineering College";

    private double defaulterThresholdPercent = 60.0; // 60% as per PDF

    private int lectureDurationMinutes = 60;

    private int qrScanWindowMinutes = 10; // final 10 minutes validation as per PDF

    private boolean otpEnabled = true;

    private boolean smsEnabled = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCollegeName() {
        return collegeName;
    }

    public void setCollegeName(String collegeName) {
        this.collegeName = collegeName;
    }

    public double getDefaulterThresholdPercent() {
        return defaulterThresholdPercent;
    }

    public void setDefaulterThresholdPercent(double defaulterThresholdPercent) {
        this.defaulterThresholdPercent = defaulterThresholdPercent;
    }

    public int getLectureDurationMinutes() {
        return lectureDurationMinutes;
    }

    public void setLectureDurationMinutes(int lectureDurationMinutes) {
        this.lectureDurationMinutes = lectureDurationMinutes;
    }

    public int getQrScanWindowMinutes() {
        return qrScanWindowMinutes;
    }

    public void setQrScanWindowMinutes(int qrScanWindowMinutes) {
        this.qrScanWindowMinutes = qrScanWindowMinutes;
    }

    public boolean isOtpEnabled() {
        return otpEnabled;
    }

    public void setOtpEnabled(boolean otpEnabled) {
        this.otpEnabled = otpEnabled;
    }

    public boolean isSmsEnabled() {
        return smsEnabled;
    }

    public void setSmsEnabled(boolean smsEnabled) {
        this.smsEnabled = smsEnabled;
    }
}
