package com.example.studentAttendanceSystemProject.controller;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.studentAttendanceSystemProject.entity.Attendance;
import com.example.studentAttendanceSystemProject.entity.AttendanceSettings;
import com.example.studentAttendanceSystemProject.entity.Department;
import com.example.studentAttendanceSystemProject.entity.PreVerifiedStudent;
import com.example.studentAttendanceSystemProject.entity.Subject;
import com.example.studentAttendanceSystemProject.entity.Teacher;
import com.example.studentAttendanceSystemProject.entity.User;
import com.example.studentAttendanceSystemProject.repository.AttendanceRepository;
import com.example.studentAttendanceSystemProject.repository.DepartmentRepository;
import com.example.studentAttendanceSystemProject.repository.PreVerifiedStudentRepository;
import com.example.studentAttendanceSystemProject.repository.UserRepository;
import com.example.studentAttendanceSystemProject.service.AdminService;
import com.example.studentAttendanceSystemProject.service.AttendanceSettingsService;
import com.example.studentAttendanceSystemProject.service.AuditLogService;
import com.example.studentAttendanceSystemProject.service.DefaulterService;
import com.example.studentAttendanceSystemProject.service.DepartmentService;
import com.example.studentAttendanceSystemProject.service.ReportExportService;
import com.example.studentAttendanceSystemProject.service.SubjectService;
import com.example.studentAttendanceSystemProject.service.TeacherService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminService adminService;

    @Autowired
    private PreVerifiedStudentRepository preVerifiedStudentRepository;

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AttendanceSettingsService settingsService;

    @Autowired
    private DefaulterService defaulterService;

    @Autowired
    private ReportExportService reportExportService;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String year,
            Model model) {

        List<PreVerifiedStudent> students;

        if (departmentId != null && year != null && !year.isEmpty()) {
            students = preVerifiedStudentRepository.findByDepartment_IdAndYear(departmentId, year);
        } else if (departmentId != null) {
            students = preVerifiedStudentRepository.findByDepartment_Id(departmentId);
        } else if (year != null && !year.isEmpty()) {
            students = preVerifiedStudentRepository.findByYear(year);
        } else {
            students = preVerifiedStudentRepository.findAll();
        }

        List<Map<String, Object>> defaulters = defaulterService.getMonthlyDefaulterList(departmentId, year, LocalDate.now());

        model.addAttribute("students", students);
        model.addAttribute("teachers", adminService.getAllTeachers());
        model.addAttribute("departments", departmentService.getAll());
        model.addAttribute("totalStudentsCount", preVerifiedStudentRepository.count());
        model.addAttribute("totalTeachersCount", teacherService.getAllTeachers().size());
        model.addAttribute("totalSubjectsCount", subjectService.getAll().size());
        model.addAttribute("defaulterCount", defaulters.size());
        model.addAttribute("settings", settingsService.getSettings());

        return "admin/dashboard";
    }

    // ====== MODULE 5: ATTENDANCE SETTINGS ======

    @GetMapping("/settings")
    public String viewSettings(Model model) {
        model.addAttribute("settings", settingsService.getSettings());
        return "admin/settings";
    }

    @PostMapping("/settings/save")
    public String saveSettings(@ModelAttribute AttendanceSettings settings, Principal principal) {
        settingsService.saveSettings(settings);
        auditLogService.log("SETTINGS_UPDATED", principal != null ? principal.getName() : "Admin", "ROLE_ADMIN",
                "Updated attendance settings (Threshold: " + settings.getDefaulterThresholdPercent()
                        + "%, Window: " + settings.getQrScanWindowMinutes() + " mins, College: " + settings.getCollegeName() + ")", "SUCCESS");
        return "redirect:/admin/settings?saved=true";
    }

    // ====== MODULE 3: STUDENT MANAGEMENT ======

    @GetMapping("/view-students")
    public String viewStudents(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String year,
            Model model) {

        List<PreVerifiedStudent> students = adminService.getFilteredStudents(departmentId, year);
        model.addAttribute("students", students);
        model.addAttribute("departments", departmentService.getAllDepartments());

        return "admin/view-students";
    }

    @PostMapping("/upload-students")
    public String uploadStudents(@RequestParam("file") MultipartFile file, Principal principal) {
        try {
            adminService.uploadStudentList(file);
            auditLogService.log("STUDENT_FILE_UPLOADED", principal != null ? principal.getName() : "Admin", "ROLE_ADMIN",
                    "Uploaded student CSV: " + file.getOriginalFilename(), "SUCCESS");
        } catch (Exception e) {
            auditLogService.log("STUDENT_FILE_UPLOAD_FAILED", principal != null ? principal.getName() : "Admin", "ROLE_ADMIN",
                    "Upload failed: " + e.getMessage(), "FAILED");
        }
        return "redirect:/admin/view-students";
    }

    @GetMapping("/edit-student/{id}")
    public String editStudent(@PathVariable Long id, Model model) {
        PreVerifiedStudent student = preVerifiedStudentRepository.findById(id).orElseThrow();
        List<Department> departments = departmentRepository.findAll();

        model.addAttribute("student", student);
        model.addAttribute("departments", departments);
        return "admin/edit-student";
    }

    @PostMapping("/update-student")
    public String updateStudent(
            @RequestParam Long id,
            @RequestParam String rollNumber,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam Long departmentId,
            @RequestParam String year,
            @RequestParam String section,
            @RequestParam(required = false) String division,
            @RequestParam(required = false) String mobileNumber,
            @RequestParam(required = false) String parentMobileNumber,
            Principal principal) {

        PreVerifiedStudent student = preVerifiedStudentRepository.findById(id).orElseThrow();
        Department dept = departmentRepository.findById(departmentId).orElseThrow();

        student.setRollNumber(rollNumber);
        student.setName(name);
        student.setEmail(email);
        student.setDepartment(dept);
        student.setYear(year);
        student.setSection(section);
        student.setDivision(division != null && !division.trim().isEmpty() ? division.trim() : section);
        student.setMobileNumber(mobileNumber);
        student.setParentMobileNumber(parentMobileNumber);

        preVerifiedStudentRepository.save(student);

        auditLogService.log("STUDENT_UPDATED", principal != null ? principal.getName() : "Admin", "ROLE_ADMIN",
                "Updated details for student: " + name + " (Roll: " + rollNumber + ", Mobile: " + mobileNumber + ")", "SUCCESS");

        return "redirect:/admin/view-students";
    }

    @GetMapping("/delete-student/{id}")
    public String deleteStudent(@PathVariable Long id, Principal principal) {
        adminService.deleteStudent(id);
        auditLogService.log("STUDENT_DELETED", principal != null ? principal.getName() : "Admin", "ROLE_ADMIN",
                "Deleted student with ID: " + id, "WARNING");
        return "redirect:/admin/view-students";
    }

    // ====== MODULE 4: TEACHER MANAGEMENT ======

    @GetMapping("/view-teachers")
    public String viewTeachers(Model model) {
        model.addAttribute("teachers", teacherService.getAllTeachers());
        return "admin/view-teachers";
    }

    @GetMapping("/add-teacher")
    public String addTeacherForm(Model model) {
        model.addAttribute("teacher", new Teacher());
        model.addAttribute("departments", departmentService.getAll());
        model.addAttribute("subjects", subjectService.getAll());
        return "admin/add-teacher";
    }

    @PostMapping("/add-teachers")
    public String addTeacher(@ModelAttribute Teacher teacher, Model model, Principal principal) {
        if (teacher.getPassword() == null || teacher.getPassword().trim().isEmpty()) {
            model.addAttribute("error", "Password cannot be empty!");
            model.addAttribute("departments", departmentService.getAll());
            return "admin/add-teacher";
        }

        Department dept = departmentService.getById(teacher.getDepartment().getId());
        teacher.setDepartment(dept);
        teacher.setSubject(null);

        teacherService.addTeacher(teacher);

        User teacherUser = new User();
        teacherUser.setName(teacher.getName());
        teacherUser.setEmail(teacher.getEmail());
        teacherUser.setPassword(teacher.getPassword());
        teacherUser.setRole("ROLE_TEACHER");
        teacherUser.setMobileNumber(teacher.getMobileNumber());
        userRepository.save(teacherUser);

        auditLogService.log("TEACHER_ADDED", principal != null ? principal.getName() : "Admin", "ROLE_ADMIN",
                "Added teacher: " + teacher.getName() + " (" + teacher.getEmail() + ")", "SUCCESS");

        return "redirect:/admin/view-teachers";
    }

    @GetMapping("/edit-teacher/{id}")
    public String editTeacher(@PathVariable Long id, Model model) {
        Teacher teacher = teacherService.getTeacherById(id);
        model.addAttribute("teacher", teacher);
        model.addAttribute("departments", departmentService.getAll());
        model.addAttribute("subjects", subjectService.getAll());
        return "admin/edit-teacher";
    }

    @PostMapping("/update-teacher")
    public String updateTeacher(@ModelAttribute Teacher teacher, Principal principal) {
        Department dept = departmentService.getById(teacher.getDepartment().getId());
        Subject sub = teacher.getSubject() != null && teacher.getSubject().getId() != null
                ? subjectService.getById(teacher.getSubject().getId()) : null;

        teacher.setDepartment(dept);
        teacher.setSubject(sub);
        teacherService.updateTeacher(teacher);

        auditLogService.log("TEACHER_UPDATED", principal != null ? principal.getName() : "Admin", "ROLE_ADMIN",
                "Updated teacher profile: " + teacher.getName(), "SUCCESS");

        return "redirect:/admin/view-teachers";
    }

    @GetMapping("/delete-teacher/{id}")
    public String deleteTeacher(@PathVariable Long id, Principal principal) {
        teacherService.deleteTeacher(id);
        auditLogService.log("TEACHER_DELETED", principal != null ? principal.getName() : "Admin", "ROLE_ADMIN",
                "Deleted teacher with ID: " + id, "WARNING");
        return "redirect:/admin/view-teachers";
    }

    // ====== MODULE 15 & 16: MONTHLY DEFAULTER LIST & MARATHI PARENT SMS ======

    @GetMapping("/defaulters")
    public String viewDefaulters(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate monthDate,
            Model model) {

        LocalDate selectedDate = monthDate != null ? monthDate : LocalDate.now();
        List<Map<String, Object>> defaulters = defaulterService.getMonthlyDefaulterList(departmentId, year, selectedDate);

        model.addAttribute("defaulters", defaulters);
        model.addAttribute("departments", departmentService.getAll());
        model.addAttribute("selectedDeptId", departmentId);
        model.addAttribute("selectedYear", year);
        model.addAttribute("selectedMonth", selectedDate);
        model.addAttribute("monthName", selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        model.addAttribute("settings", settingsService.getSettings());

        return "admin/defaulters";
    }

    @PostMapping("/sms/send/{studentId}")
    public String sendMarathiSms(@PathVariable Long studentId,
                                 @RequestParam(required = false) Double percentage,
                                 @RequestParam(required = false) String monthName,
                                 Principal principal) {
        PreVerifiedStudent student = preVerifiedStudentRepository.findById(studentId).orElseThrow();
        double pct = percentage != null ? percentage : 50.0;
        String mName = monthName != null ? monthName : LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"));

        defaulterService.sendMarathiSmsToParent(student, pct, mName, principal != null ? principal.getName() : "Admin");
        return "redirect:/admin/defaulters?smsSuccess=true";
    }

    @PostMapping("/sms/send-bulk")
    public String sendBulkMarathiSms(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String year,
            Principal principal) {
        int count = defaulterService.sendBulkMarathiSmsToDefaulters(departmentId, year, LocalDate.now(),
                principal != null ? principal.getName() : "Admin");
        return "redirect:/admin/defaulters?bulkSent=" + count;
    }

    @GetMapping("/defaulters/remove/{studentId}")
    public String removeDefaulter(@PathVariable Long studentId, Principal principal) {
        defaulterService.removeStudentFromDefaulters(studentId);
        auditLogService.log("DEFAULTER_REMOVED", principal != null ? principal.getName() : "Admin", "ROLE_ADMIN",
                "Removed student ID " + studentId + " from defaulter list", "SUCCESS");
        return "redirect:/admin/defaulters?removed=true";
    }

    @GetMapping("/defaulters/clear")
    public String clearDefaulters(Principal principal) {
        defaulterService.clearAllDefaulters();
        auditLogService.log("DEFAULTERS_CLEARED", principal != null ? principal.getName() : "Admin", "ROLE_ADMIN",
                "Cleared all monthly defaulters", "SUCCESS");
        return "redirect:/admin/defaulters?cleared=true";
    }

    @GetMapping("/sms/delete/{logId}")
    public String deleteSmsLog(@PathVariable Long logId, Principal principal) {
        defaulterService.deleteSmsLog(logId);
        return "redirect:/admin/sms/logs?deleted=true";
    }

    @GetMapping("/sms/clear")
    public String clearSmsLogs(Principal principal) {
        defaulterService.clearAllSmsLogs();
        return "redirect:/admin/sms/logs?cleared=true";
    }

    @GetMapping("/sms/logs")
    public String smsLogs(Model model) {
        model.addAttribute("smsLogs", defaulterService.getRecentSmsLogs());
        return "admin/sms-logs";
    }

    // ====== MODULE 22: AUDIT TRAIL & SECURITY ======

    @GetMapping("/audit-logs")
    public String viewAuditLogs(Model model) {
        model.addAttribute("auditLogs", auditLogService.getRecentLogs());
        return "admin/audit-logs";
    }

    // ====== MODULE 19 & 20: REPORTS & EXCEL/PDF EXPORT ======

    @GetMapping("/reports")
    public String reports(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {

        List<Attendance> attendances;
        if (date != null) {
            attendances = attendanceRepository.findByDate(date);
        } else {
            attendances = attendanceRepository.findAll();
        }

        if (departmentId != null) {
            attendances = attendances.stream()
                    .filter(a -> a.getPstudent() != null && a.getPstudent().getDepartment() != null
                            && a.getPstudent().getDepartment().getId().equals(departmentId))
                    .toList();
        }

        model.addAttribute("attendances", attendances);
        model.addAttribute("departments", departmentService.getAll());
        model.addAttribute("selectedDeptId", departmentId);
        model.addAttribute("selectedDate", date);

        return "admin/reports";
    }

    @GetMapping("/reports/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) throws IOException {

        List<Attendance> attendances = (date != null) ? attendanceRepository.findByDate(date) : attendanceRepository.findAll();
        if (departmentId != null) {
            attendances = attendances.stream()
                    .filter(a -> a.getPstudent() != null && a.getPstudent().getDepartment() != null
                            && a.getPstudent().getDepartment().getId().equals(departmentId))
                    .toList();
        }

        byte[] excelBytes = reportExportService.exportAttendanceListToExcel("College Attendance Report", attendances);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=college_attendance_report.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    @GetMapping("/reports/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) throws Exception {

        List<Attendance> attendances = (date != null) ? attendanceRepository.findByDate(date) : attendanceRepository.findAll();
        if (departmentId != null) {
            attendances = attendances.stream()
                    .filter(a -> a.getPstudent() != null && a.getPstudent().getDepartment() != null
                            && a.getPstudent().getDepartment().getId().equals(departmentId))
                    .toList();
        }

        byte[] pdfBytes = reportExportService.exportAttendanceListToPdf("College Attendance Report", attendances);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=college_attendance_report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/defaulters/export/excel")
    public ResponseEntity<byte[]> exportDefaultersExcel(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String year) throws IOException {

        List<Map<String, Object>> defaulters = defaulterService.getMonthlyDefaulterList(departmentId, year, LocalDate.now());
        byte[] excelBytes = reportExportService.exportDefaultersToExcel(
                "College Monthly Defaulter List (< 60% Attendance)", defaulters);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=college_defaulters.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    @GetMapping("/defaulters/export/pdf")
    public ResponseEntity<byte[]> exportDefaultersPdf(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String year) throws Exception {

        List<Map<String, Object>> defaulters = defaulterService.getMonthlyDefaulterList(departmentId, year, LocalDate.now());
        byte[] pdfBytes = reportExportService.exportDefaultersToPdf(
                "College Monthly Defaulter List (< 60% Attendance)", defaulters);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=college_defaulters.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
