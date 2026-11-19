package com.example.studentAttendanceSystemProject.controller;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import com.example.studentAttendanceSystemProject.entity.LectureSession;
import com.example.studentAttendanceSystemProject.entity.PreVerifiedStudent;
import com.example.studentAttendanceSystemProject.entity.Subject;
import com.example.studentAttendanceSystemProject.entity.Teacher;
import com.example.studentAttendanceSystemProject.repository.AttendanceRepository;
import com.example.studentAttendanceSystemProject.repository.PreVerifiedStudentRepository;
import com.example.studentAttendanceSystemProject.repository.SubjectRepository;
import com.example.studentAttendanceSystemProject.service.AttendanceService;
import com.example.studentAttendanceSystemProject.service.AttendanceSettingsService;
import com.example.studentAttendanceSystemProject.service.AuditLogService;
import com.example.studentAttendanceSystemProject.service.DefaulterService;
import com.example.studentAttendanceSystemProject.service.LectureSessionService;
import com.example.studentAttendanceSystemProject.service.ReportExportService;
import com.example.studentAttendanceSystemProject.service.SubjectService;
import com.example.studentAttendanceSystemProject.service.TeacherService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/teacher")
public class TeacherController {

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private PreVerifiedStudentRepository studentRepository;

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private LectureSessionService lectureSessionService;

    @Autowired
    private DefaulterService defaulterService;

    @Autowired
    private ReportExportService reportExportService;

    @Autowired
    private AttendanceSettingsService settingsService;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        Teacher teacher = teacherService.getByEmail(principal.getName());
        List<Subject> subjects = subjectService.getByTeacher(teacher);
        AttendanceSettings settings = settingsService.getSettings();

        List<Map<String, Object>> studentAttendanceList = new ArrayList<>();
        int defaulterCount = 0;

        for (Subject subject : subjects) {
            List<PreVerifiedStudent> students = studentRepository.findByDepartmentAndYear(
                    subject.getDepartment(), subject.getYear());

            long totalClasses = attendanceRepository.countBySubject(subject);

            for (PreVerifiedStudent student : students) {
                long presentCount = attendanceRepository.countByStudentAndSubject(student, subject);
                double percent = totalClasses > 0 ? (presentCount * 100.0) / totalClasses : 0.0;
                boolean isLow = percent < settings.getDefaulterThresholdPercent();
                if (isLow && totalClasses > 0) defaulterCount++;

                Map<String, Object> map = new HashMap<>();
                map.put("student", student);
                map.put("subject", subject);
                map.put("attendancePercent", percent);
                map.put("lowAttendance", isLow);

                studentAttendanceList.add(map);
            }
        }

        List<LectureSession> recentSessions = lectureSessionService.getSessionsByTeacher(teacher);

        model.addAttribute("teacher", teacher);
        model.addAttribute("subjects", subjects);
        model.addAttribute("studentAttendanceList", studentAttendanceList);
        model.addAttribute("recentSessions", recentSessions);
        model.addAttribute("defaulterCount", defaulterCount);
        model.addAttribute("settings", settings);

        return "teacher/dashboard";
    }

    // ====== MODULE 7 & 8: PER-LECTURE QR GENERATION ======

    @GetMapping("/lecture/new")
    public String newLectureForm(Model model, Principal principal) {
        Teacher teacher = teacherService.getByEmail(principal.getName());
        List<Subject> subjects = subjectService.getByTeacher(teacher);
        AttendanceSettings settings = settingsService.getSettings();

        model.addAttribute("teacher", teacher);
        model.addAttribute("subjects", subjects);
        model.addAttribute("settings", settings);
        return "teacher/lecture-new";
    }

    @PostMapping("/lecture/start")
    public String startLecture(@RequestParam Long subjectId,
                               @RequestParam(required = false) String division,
                               @RequestParam(required = false) Integer durationMinutes,
                               @RequestParam(required = false) Integer windowMinutes,
                               Principal principal) {
        LectureSession session = lectureSessionService.startLectureSession(
                subjectId, principal.getName(), division, durationMinutes, windowMinutes);
        return "redirect:/teacher/lecture/live/" + session.getId();
    }

    public static String getLocalLanIp() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                java.util.Enumeration<java.net.InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress addr = addresses.nextElement();
                    if (addr instanceof java.net.Inet4Address && !addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        if (!ip.startsWith("169.254")) {
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return "localhost";
    }

    @GetMapping("/lecture/live/{id}")
    public String liveLectureSession(@PathVariable Long id, Model model, HttpServletRequest request, Principal principal) {
        Teacher teacher = teacherService.getByEmail(principal.getName());
        Optional<LectureSession> sessionOpt = lectureSessionService.getById(id);
        if (sessionOpt.isEmpty()) {
            return "redirect:/teacher/dashboard";
        }

        LectureSession session = sessionOpt.get();

        // Build baseUrl using LAN IP so phones can connect directly
        String hostHeader = request.getHeader("Host");
        int port = request.getServerPort() > 0 ? request.getServerPort() : 3217;
        String baseUrl;
        String lanIp = getLocalLanIp();

        if (hostHeader != null && !hostHeader.isEmpty() && !hostHeader.contains("localhost") && !hostHeader.contains("127.0.0.1")) {
            baseUrl = request.getScheme() + "://" + hostHeader;
        } else {
            baseUrl = request.getScheme() + "://" + lanIp + ":" + port;
        }

        String qrImage = lectureSessionService.getQrCodeImage(session, baseUrl);
        String scanUrl = baseUrl + "/attend?token=" + session.getQrToken();
        String rawToken = session.getQrToken();

        List<Attendance> attendances = attendanceRepository.findByLectureSession(session);
        long presentCount = attendances.stream().filter(Attendance::isPresent).count();

        model.addAttribute("teacher", teacher);
        model.addAttribute("lectureSession", session);
        model.addAttribute("session", session);
        model.addAttribute("qrImage", qrImage);
        model.addAttribute("scanUrl", scanUrl);
        model.addAttribute("rawToken", rawToken);
        model.addAttribute("lanIp", lanIp);
        model.addAttribute("attendances", attendances);
        model.addAttribute("presentCount", presentCount);
        model.addAttribute("scanUrl", scanUrl);
        model.addAttribute("rawToken", rawToken);

        return "teacher/lecture-live";
    }

    // Module 12: Manual QR Code Expiry
    @PostMapping("/lecture/close/{id}")
    public String closeLecture(@PathVariable Long id, Principal principal) {
        lectureSessionService.closeLectureSession(id, principal.getName());
        return "redirect:/teacher/lecture/live/" + id;
    }

    // ====== MODULE 13: MANUAL ATTENDANCE & CORRECTION ======

    @GetMapping("/attendance/{subjectId}")
    public String attendanceForm(@PathVariable Long subjectId, Model model, Principal principal) {
        Teacher teacher = teacherService.getByEmail(principal.getName());
        Subject subject = subjectService.getById(subjectId);

        List<PreVerifiedStudent> students = studentRepository
                .findByDepartment_IdAndYear(subject.getDepartment().getId(), subject.getYear());

        LocalDate today = LocalDate.now();
        List<Attendance> attendances = attendanceRepository.findByTeacherAndSubjectAndDate(teacher, subject, today);

        Map<Long, Boolean> attendanceMap = new HashMap<>();
        for (Attendance a : attendances) {
            attendanceMap.put(a.getPstudent().getId(), a.isPresent());
        }

        model.addAttribute("teacher", teacher);
        model.addAttribute("subject", subject);
        model.addAttribute("students", students);
        model.addAttribute("attendanceMap", attendanceMap);
        return "teacher/attendance-form";
    }

    @PostMapping("/attendance/save")
    public String saveAttendance(
            @RequestParam("subjectId") Long subjectId,
            @RequestParam("studentIds") List<Long> studentIds,
            @RequestParam(value = "presentList", required = false) List<String> presentList,
            Principal principal) {

        Teacher teacher = teacherService.getByEmail(principal.getName());
        Subject subject = subjectService.getById(subjectId);
        LocalDate today = LocalDate.now();

        for (Long studentId : studentIds) {
            PreVerifiedStudent student = studentRepository.findById(studentId).get();
            boolean present = presentList != null && presentList.contains(student.getId().toString());

            Attendance existing = attendanceRepository
                .findByPstudentIdAndSubjectIdAndDateAndTeacherId(
                    student.getId(),
                    subject.getId(),
                    today,
                    teacher.getId()
                );

            if (existing != null) {
                boolean wasPresent = existing.isPresent();
                existing.setPresent(present);
                existing.setMarkedVia("MANUAL_TEACHER");
                attendanceRepository.save(existing);

                if (wasPresent != present) {
                    auditLogService.log("ATTENDANCE_CORRECTION", teacher.getEmail(), "ROLE_TEACHER",
                            "Corrected attendance for " + student.getName() + " (Roll: " + student.getRollNumber()
                                    + ") in " + subject.getSubjectName() + " to: " + (present ? "PRESENT" : "ABSENT"), "SUCCESS");
                }
            } else {
                Attendance attendance = new Attendance();
                attendance.setTeacher(teacher);
                attendance.setPstudent(student);
                attendance.setSubject(subject);
                attendance.setDate(today);
                attendance.setPresent(present);
                attendance.setMarkedVia("MANUAL_TEACHER");
                attendanceRepository.save(attendance);

                auditLogService.log("MANUAL_ATTENDANCE_MARKED", teacher.getEmail(), "ROLE_TEACHER",
                        "Manual attendance marked for " + student.getName() + " (" + (present ? "PRESENT" : "ABSENT") + ")", "SUCCESS");
            }
        }

        return "redirect:/teacher/dashboard";
    }

    // ====== MODULE 15 & 16: MONTHLY DEFAULTER LIST & MARATHI PARENT SMS ======

    @GetMapping("/defaulters")
    public String viewDefaulters(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate monthDate,
            Model model, Principal principal) {

        Teacher teacher = teacherService.getByEmail(principal.getName());
        List<Subject> subjects = subjectService.getByTeacher(teacher);

        LocalDate selectedDate = monthDate != null ? monthDate : LocalDate.now();
        Long deptId = teacher.getDepartment() != null ? teacher.getDepartment().getId() : null;

        List<Map<String, Object>> defaulters = defaulterService.getMonthlyDefaulterList(deptId, null, selectedDate);

        model.addAttribute("teacher", teacher);
        model.addAttribute("subjects", subjects);
        model.addAttribute("defaulters", defaulters);
        model.addAttribute("selectedMonth", selectedDate);
        model.addAttribute("monthName", selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        model.addAttribute("settings", settingsService.getSettings());

        return "teacher/defaulters";
    }

    @PostMapping("/sms/send/{studentId}")
    public String sendMarathiSms(@PathVariable Long studentId,
                                 @RequestParam(required = false) Double percentage,
                                 @RequestParam(required = false) String monthName,
                                 Principal principal) {
        PreVerifiedStudent student = studentRepository.findById(studentId).orElseThrow();
        double pct = percentage != null ? percentage : 50.0;
        String mName = monthName != null ? monthName : LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"));

        defaulterService.sendMarathiSmsToParent(student, pct, mName, principal.getName());
        return "redirect:/teacher/defaulters?smsSuccess=true";
    }

    @PostMapping("/sms/send-bulk")
    public String sendBulkMarathiSms(Principal principal) {
        Teacher teacher = teacherService.getByEmail(principal.getName());
        Long deptId = teacher.getDepartment() != null ? teacher.getDepartment().getId() : null;
        int count = defaulterService.sendBulkMarathiSmsToDefaulters(deptId, null, LocalDate.now(), principal.getName());
        return "redirect:/teacher/defaulters?bulkSent=" + count;
    }

    @GetMapping("/defaulters/remove/{studentId}")
    public String removeDefaulter(@PathVariable Long studentId, Principal principal) {
        defaulterService.removeStudentFromDefaulters(studentId);
        return "redirect:/teacher/defaulters?removed=true";
    }

    @GetMapping("/defaulters/clear")
    public String clearDefaulters(Principal principal) {
        defaulterService.clearAllDefaulters();
        return "redirect:/teacher/defaulters?cleared=true";
    }

    @GetMapping("/sms/delete/{logId}")
    public String deleteSmsLog(@PathVariable Long logId, Principal principal) {
        defaulterService.deleteSmsLog(logId);
        return "redirect:/teacher/sms/logs?deleted=true";
    }

    @GetMapping("/sms/clear")
    public String clearSmsLogs(Principal principal) {
        defaulterService.clearAllSmsLogs();
        return "redirect:/teacher/sms/logs?cleared=true";
    }

    @GetMapping("/sms/logs")
    public String smsLogs(Model model, Principal principal) {
        Teacher teacher = teacherService.getByEmail(principal.getName());
        model.addAttribute("teacher", teacher);
        model.addAttribute("smsLogs", defaulterService.getRecentSmsLogs());
        return "teacher/sms-logs";
    }

    // ====== MODULE 19 & 20: REPORT GENERATION & EXCEL/PDF EXPORT ======

    @GetMapping("/attendance/report")
    public String attendanceReport(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model, Principal principal) {

        Teacher teacher = teacherService.getByEmail(principal.getName());
        List<Attendance> attendances;

        if (subjectId != null && date != null) {
            Subject subject = subjectService.getById(subjectId);
            attendances = attendanceRepository.findByTeacherAndSubjectAndDate(teacher, subject, date);
        } else if (date != null) {
            attendances = attendanceRepository.findByTeacherAndDate(teacher, date);
        } else {
            attendances = attendanceRepository.findByTeacher(teacher);
        }

        model.addAttribute("attendances", attendances);
        model.addAttribute("subjects", subjectRepository.findByTeacher(teacher));
        model.addAttribute("teacher", teacher);
        model.addAttribute("selectedSubjectId", subjectId);
        model.addAttribute("selectedDate", date);

        return "teacher/attendance-report";
    }

    @GetMapping("/attendance/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Principal principal) throws IOException {

        Teacher teacher = teacherService.getByEmail(principal.getName());
        List<Attendance> attendances;
        String title = "Attendance Report - " + teacher.getName();

        if (subjectId != null && date != null) {
            Subject subject = subjectService.getById(subjectId);
            attendances = attendanceRepository.findByTeacherAndSubjectAndDate(teacher, subject, date);
            title += " (" + subject.getSubjectName() + " on " + date + ")";
        } else {
            attendances = attendanceRepository.findByTeacher(teacher);
        }

        byte[] excelBytes = reportExportService.exportAttendanceListToExcel(title, attendances);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=attendance_report.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    @GetMapping("/attendance/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Principal principal) throws Exception {

        Teacher teacher = teacherService.getByEmail(principal.getName());
        List<Attendance> attendances;
        String title = "Attendance Report - " + teacher.getName();

        if (subjectId != null && date != null) {
            Subject subject = subjectService.getById(subjectId);
            attendances = attendanceRepository.findByTeacherAndSubjectAndDate(teacher, subject, date);
            title += " (" + subject.getSubjectName() + " on " + date + ")";
        } else {
            attendances = attendanceRepository.findByTeacher(teacher);
        }

        byte[] pdfBytes = reportExportService.exportAttendanceListToPdf(title, attendances);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=attendance_report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/defaulters/export/excel")
    public ResponseEntity<byte[]> exportDefaultersExcel(Principal principal) throws IOException {
        Teacher teacher = teacherService.getByEmail(principal.getName());
        Long deptId = teacher.getDepartment() != null ? teacher.getDepartment().getId() : null;
        List<Map<String, Object>> defaulters = defaulterService.getMonthlyDefaulterList(deptId, null, LocalDate.now());

        byte[] excelBytes = reportExportService.exportDefaultersToExcel(
                "Monthly Defaulter List (< 60% Attendance)", defaulters);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=defaulter_list.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    @GetMapping("/defaulters/export/pdf")
    public ResponseEntity<byte[]> exportDefaultersPdf(Principal principal) throws Exception {
        Teacher teacher = teacherService.getByEmail(principal.getName());
        Long deptId = teacher.getDepartment() != null ? teacher.getDepartment().getId() : null;
        List<Map<String, Object>> defaulters = defaulterService.getMonthlyDefaulterList(deptId, null, LocalDate.now());

        byte[] pdfBytes = reportExportService.exportDefaultersToPdf(
                "Monthly Defaulter List (< 60% Attendance)", defaulters);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=defaulter_list.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
