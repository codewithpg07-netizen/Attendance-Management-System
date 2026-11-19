package com.example.studentAttendanceSystemProject.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.studentAttendanceSystemProject.entity.AttendanceSettings;
import com.example.studentAttendanceSystemProject.repository.AttendanceSettingsRepository;

@Service
public class AttendanceSettingsService {

    @Autowired
    private AttendanceSettingsRepository settingsRepository;

    public AttendanceSettings getSettings() {
        List<AttendanceSettings> all = settingsRepository.findAll();
        if (all.isEmpty()) {
            AttendanceSettings defaultSettings = new AttendanceSettings();
            defaultSettings.setCollegeName("Government Polytechnic / Engineering College");
            defaultSettings.setDefaulterThresholdPercent(60.0);
            defaultSettings.setLectureDurationMinutes(60);
            defaultSettings.setQrScanWindowMinutes(10);
            defaultSettings.setOtpEnabled(true);
            defaultSettings.setSmsEnabled(true);
            return settingsRepository.save(defaultSettings);
        }
        return all.get(0);
    }

    public AttendanceSettings saveSettings(AttendanceSettings settings) {
        AttendanceSettings existing = getSettings();
        existing.setCollegeName(settings.getCollegeName());
        existing.setDefaulterThresholdPercent(settings.getDefaulterThresholdPercent());
        existing.setLectureDurationMinutes(settings.getLectureDurationMinutes());
        existing.setQrScanWindowMinutes(settings.getQrScanWindowMinutes());
        existing.setOtpEnabled(settings.isOtpEnabled());
        existing.setSmsEnabled(settings.isSmsEnabled());
        return settingsRepository.save(existing);
    }
}
