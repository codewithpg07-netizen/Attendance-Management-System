package com.example.studentAttendanceSystemProject.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.studentAttendanceSystemProject.entity.Attendance;
import com.example.studentAttendanceSystemProject.entity.AttendanceSettings;
import com.example.studentAttendanceSystemProject.entity.Department;
import com.example.studentAttendanceSystemProject.entity.PreVerifiedStudent;
import com.example.studentAttendanceSystemProject.entity.SmsAlertLog;
import com.example.studentAttendanceSystemProject.entity.Subject;
import com.example.studentAttendanceSystemProject.repository.AttendanceRepository;
import com.example.studentAttendanceSystemProject.repository.PreVerifiedStudentRepository;
import com.example.studentAttendanceSystemProject.repository.SmsAlertLogRepository;
import com.example.studentAttendanceSystemProject.repository.SubjectRepository;

@Service
public class DefaulterService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private PreVerifiedStudentRepository studentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private SmsAlertLogRepository smsAlertLogRepository;

    @Autowired
    private AttendanceSettingsService settingsService;

    @Autowired
    private AuditLogService auditLogService;

    /**
     * Calculates monthly attendance stats for all students in given department / year / month.
     */
    public List<Map<String, Object>> getMonthlyAttendanceRecords(Long departmentId, String year, LocalDate monthDate) {
        LocalDate startOfMonth = monthDate.withDayOfMonth(1);
        LocalDate endOfMonth = monthDate.withDayOfMonth(monthDate.lengthOfMonth());

        List<PreVerifiedStudent> students;
        if (departmentId != null && year != null && !year.trim().isEmpty()) {
            students = studentRepository.findByDepartment_IdAndYear(departmentId, year.trim());
        } else if (departmentId != null) {
            students = studentRepository.findByDepartment_Id(departmentId);
        } else if (year != null && !year.trim().isEmpty()) {
            students = studentRepository.findByYear(year.trim());
        } else {
            students = studentRepository.findAll();
        }

        AttendanceSettings settings = settingsService.getSettings();
        double threshold = settings.getDefaulterThresholdPercent();

        List<Map<String, Object>> records = new ArrayList<>();

        for (PreVerifiedStudent student : students) {
            List<Attendance> monthlyAttendances = attendanceRepository
                    .findByPstudentAndDateBetween(student, startOfMonth, endOfMonth);

            long totalClasses = monthlyAttendances.size();
            long attendedClasses = monthlyAttendances.stream().filter(Attendance::isPresent).count();

            double percentage = totalClasses > 0 ? (attendedClasses * 100.0) / totalClasses : 0.0;
            boolean isDefaulter = totalClasses > 0 && percentage < threshold;

            Map<String, Object> map = new HashMap<>();
            map.put("student", student);
            map.put("totalClasses", totalClasses);
            map.put("attendedClasses", attendedClasses);
            map.put("attendancePercentage", percentage);
            map.put("percentageFormatted", String.format("%.1f%%", percentage));
            map.put("isDefaulter", isDefaulter);
            map.put("monthName", monthDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")));

            records.add(map);
        }

        return records;
    }

    /**
     * Module 15: Returns only the Defaulter list (monthly attendance < 60%).
     */
    public List<Map<String, Object>> getMonthlyDefaulterList(Long departmentId, String year, LocalDate monthDate) {
        List<Map<String, Object>> allRecords = getMonthlyAttendanceRecords(departmentId, year, monthDate);
        List<Map<String, Object>> defaulters = new ArrayList<>();

        for (Map<String, Object> rec : allRecords) {
            if ((boolean) rec.get("isDefaulter")) {
                defaulters.add(rec);
            }
        }
        return defaulters;
    }

    /**
     * Module 18: Calculates semester-wise percentage for a student.
     */
    public Map<String, Object> calculateSemesterAttendance(PreVerifiedStudent student) {
        List<Attendance> all = attendanceRepository.findByPstudent(student);
        long total = all.size();
        long present = all.stream().filter(Attendance::isPresent).count();
        double percent = total > 0 ? (present * 100.0) / total : 0.0;

        Map<String, Object> result = new HashMap<>();
        result.put("totalClasses", total);
        result.put("attendedClasses", present);
        result.put("percentage", percent);
        result.put("percentageFormatted", String.format("%.1f%%", percent));
        result.put("isDefaulter", total > 0 && percent < settingsService.getSettings().getDefaulterThresholdPercent());
        return result;
    }

    /**
     * Module 16: Builds SMS text sending student name only.
     */
    public String generateMarathiSmsMessage(PreVerifiedStudent student, double attendancePercentage) {
        return student.getName();
    }

    /**
     * Module 16: Send Marathi SMS to parent of a single student.
     */
    @Transactional
    public SmsAlertLog sendMarathiSmsToParent(PreVerifiedStudent student, double percentage, String monthYear, String initiatedBy) {
        String parentMobile = student.getParentMobileNumber();
        if (parentMobile == null || parentMobile.trim().isEmpty()) {
            parentMobile = student.getMobileNumber() != null ? student.getMobileNumber() : "पालकांचा नंबर अनुपलब्ध";
        }

        String message = generateMarathiSmsMessage(student, percentage);

        SmsAlertLog log = new SmsAlertLog(student, parentMobile, monthYear, percentage, message, "SENT");
        SmsAlertLog saved = smsAlertLogRepository.save(log);

        auditLogService.log("MARATHI_PARENT_SMS_SENT", initiatedBy, "TEACHER_OR_ADMIN",
                "Sent Marathi Attendance Alert SMS to parent of " + student.getName() + " (" + parentMobile + "): "
                        + String.format("%.1f%%", percentage), "SUCCESS");

        return saved;
    }

    /**
     * Module 16: Bulk send Marathi SMS to all monthly defaulters.
     */
    @Transactional
    public int sendBulkMarathiSmsToDefaulters(Long departmentId, String year, LocalDate monthDate, String initiatedBy) {
        List<Map<String, Object>> defaulters = getMonthlyDefaulterList(departmentId, year, monthDate);
        String monthYear = monthDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"));

        int sentCount = 0;
        for (Map<String, Object> item : defaulters) {
            PreVerifiedStudent student = (PreVerifiedStudent) item.get("student");
            double pct = (double) item.get("attendancePercentage");
            sendMarathiSmsToParent(student, pct, monthYear, initiatedBy);
            sentCount++;
        }

        return sentCount;
    }

    public List<SmsAlertLog> getRecentSmsLogs() {
        return smsAlertLogRepository.findTop100ByOrderBySentAtDesc();
    }

    /**
     * Remove a student from defaulter list by deleting their absent attendance records for the current month.
     */
    @Transactional
    public void removeStudentFromDefaulters(Long studentId) {
        PreVerifiedStudent student = studentRepository.findById(studentId).orElse(null);
        if (student != null) {
            LocalDate today = LocalDate.now();
            LocalDate startOfMonth = today.withDayOfMonth(1);
            LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
            List<Attendance> attendances = attendanceRepository.findByPstudentAndDateBetween(student, startOfMonth, endOfMonth);
            for (Attendance a : attendances) {
                if (!a.isPresent()) {
                    attendanceRepository.delete(a);
                }
            }
        }
    }

    /**
     * Clear all defaulters by removing absent records for current month.
     */
    @Transactional
    public void clearAllDefaulters() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        List<Attendance> attendances = attendanceRepository.findByDateBetween(startOfMonth, endOfMonth);
        for (Attendance a : attendances) {
            if (!a.isPresent()) {
                attendanceRepository.delete(a);
            }
        }
    }

    /**
     * Delete a single SMS log by ID.
     */
    @Transactional
    public void deleteSmsLog(Long logId) {
        smsAlertLogRepository.deleteById(logId);
    }

    /**
     * Clear all SMS logs history.
     */
    @Transactional
    public void clearAllSmsLogs() {
        smsAlertLogRepository.deleteAll();
    }
}
