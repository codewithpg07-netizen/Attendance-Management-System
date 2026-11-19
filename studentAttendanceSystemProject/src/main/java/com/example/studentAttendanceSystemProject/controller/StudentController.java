package com.example.studentAttendanceSystemProject.controller;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.studentAttendanceSystemProject.entity.Attendance;
import com.example.studentAttendanceSystemProject.entity.AttendanceSettings;
import com.example.studentAttendanceSystemProject.entity.PreVerifiedStudent;
import com.example.studentAttendanceSystemProject.entity.Subject;
import com.example.studentAttendanceSystemProject.service.AttendanceService;
import com.example.studentAttendanceSystemProject.service.AttendanceSettingsService;
import com.example.studentAttendanceSystemProject.service.DefaulterService;
import com.example.studentAttendanceSystemProject.service.LectureSessionService;
import com.example.studentAttendanceSystemProject.service.StudentService;
import com.example.studentAttendanceSystemProject.service.SubjectService;

import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/student")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private LectureSessionService lectureSessionService;

    @Autowired
    private DefaulterService defaulterService;

    @Autowired
    private AttendanceSettingsService settingsService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        PreVerifiedStudent student = studentService.getStudentByEmail(principal.getName());
        AttendanceSettings settings = settingsService.getSettings();

        List<Subject> subjects = subjectService.findByDepartmentAndYear(
                student.getDepartment(),
                student.getYear()
        );

        List<Map<String, Object>> attendanceList = new ArrayList<>();
        boolean hasLowAttendance = false;

        for (Subject sub : subjects) {
            Map<String, Object> map = new HashMap<>();
            map.put("subject", sub);

            double percent = studentService.calculateAttendance(student, sub);
            map.put("attendancePercent", percent);
            map.put("attendancePercentFormatted", String.format("%.1f%%", percent));
            boolean isLow = percent < settings.getDefaulterThresholdPercent();
            map.put("lowAttendance", isLow);
            if (isLow) hasLowAttendance = true;

            attendanceList.add(map);
        }

        // Semester-wide percentage calculation
        Map<String, Object> semesterStats = defaulterService.calculateSemesterAttendance(student);

        // Recent attendances
        List<Attendance> recentAttendances = attendanceService.getByStudent(student);
        if (recentAttendances.size() > 10) {
            recentAttendances = recentAttendances.subList(recentAttendances.size() - 10, recentAttendances.size());
        }

        model.addAttribute("student", student);
        model.addAttribute("subjects", subjects);
        model.addAttribute("attendanceList", attendanceList);
        model.addAttribute("semesterStats", semesterStats);
        model.addAttribute("recentAttendances", recentAttendances);
        model.addAttribute("hasLowAttendance", hasLowAttendance);
        model.addAttribute("settings", settings);
        model.addAttribute("missingMobile", student.getMobileNumber() == null || student.getMobileNumber().trim().isEmpty());

        return "student/dashboard";
    }

    // ====== MODULE 8: DYNAMIC QR SCANNER ======

    @GetMapping("/scan-qr")
    public String scanQrPage(@RequestParam(required = false) String token, Model model, Principal principal) {
        PreVerifiedStudent student = studentService.getStudentByEmail(principal.getName());
        model.addAttribute("student", student);
        model.addAttribute("initialToken", token != null ? token.trim() : "");
        model.addAttribute("hasRegisteredMobile", student.getMobileNumber() != null && !student.getMobileNumber().trim().isEmpty());
        return "student/scan-qr";
    }

    @PostMapping("/scan-qr")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> processQrScan(@RequestParam("token") String token, Principal principal) {
        // Robustly extract token from either a bare token or a full/partial URL
        String cleanToken = token.trim();

        // If it looks like a URL, parse the token= parameter properly
        if (cleanToken.startsWith("http://") || cleanToken.startsWith("https://")) {
            try {
                java.net.URI uri = java.net.URI.create(cleanToken);
                String query = uri.getQuery();
                if (query != null) {
                    for (String part : query.split("&")) {
                        if (part.startsWith("token=")) {
                            cleanToken = part.substring(6);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                // fall through to string-based extraction below
            }
        }
        // Fallback: string-based extraction for partial URLs
        if (cleanToken.contains("token=")) {
            cleanToken = cleanToken.substring(cleanToken.indexOf("token=") + 6);
        }
        if (cleanToken.contains("&")) {
            cleanToken = cleanToken.substring(0, cleanToken.indexOf("&"));
        }
        // Strip any trailing whitespace or slashes
        cleanToken = cleanToken.trim().replaceAll("[\r\n /]", "");

        Map<String, Object> response = lectureSessionService.validateAndMarkAttendance(cleanToken, principal.getName());
        return ResponseEntity.ok(response);
    }

    // ====== EXISTING DETAILS & EXPORT ======

    @GetMapping("/subject/{subjectId}/attendance")
    public String subjectAttendanceDetail(@PathVariable Long subjectId, Model model, Principal principal) {
        PreVerifiedStudent student = studentService.getStudentByEmail(principal.getName());
        Subject subject = subjectService.getById(subjectId);

        List<Attendance> dailyAttendance = attendanceService.getByStudentAndSubject(student, subject);

        model.addAttribute("student", student);
        model.addAttribute("subject", subject);
        model.addAttribute("dailyAttendance", dailyAttendance);

        return "student/subject-attendance";
    }

    @GetMapping("/attendance/download/excel/{subjectId}")
    public void downloadAttendanceExcel(@PathVariable Long subjectId,
                                        HttpServletResponse response,
                                        Principal principal) throws IOException {

        PreVerifiedStudent student = studentService.getStudentByEmail(principal.getName());
        Subject subject = subjectService.getById(subjectId);

        List<Attendance> attendances = studentService.getAttendanceByStudentAndSubject(student, subject);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=attendance_" + subject.getSubjectName() + ".xlsx");

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Attendance");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Date");
        header.createCell(1).setCellValue("Status");
        header.createCell(2).setCellValue("Marked Via");

        int rowIdx = 1;
        for (Attendance att : attendances) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(att.getDate().toString());
            row.createCell(1).setCellValue(att.isPresent() ? "Present" : "Absent");
            row.createCell(2).setCellValue(att.getMarkedVia() != null ? att.getMarkedVia() : "MANUAL");
        }

        workbook.write(response.getOutputStream());
        workbook.close();
    }

    @GetMapping("/attendance/{subjectId}")
    public String attendanceDetail(@PathVariable Long subjectId, Model model, Principal principal) {
        PreVerifiedStudent student = studentService.getStudentByEmail(principal.getName());
        Subject subject = subjectService.getById(subjectId);

        List<Attendance> attendanceList = studentService.getAttendanceByStudentAndSubject(student, subject);

        model.addAttribute("student", student);
        model.addAttribute("subject", subject);
        model.addAttribute("attendanceList", attendanceList);

        return "student/attendance-detail";
    }
}
