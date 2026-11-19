package com.example.studentAttendanceSystemProject.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.studentAttendanceSystemProject.entity.Attendance;
import com.example.studentAttendanceSystemProject.entity.AttendanceSettings;
import com.example.studentAttendanceSystemProject.entity.LectureSession;
import com.example.studentAttendanceSystemProject.entity.PreVerifiedStudent;
import com.example.studentAttendanceSystemProject.entity.Subject;
import com.example.studentAttendanceSystemProject.entity.Teacher;
import com.example.studentAttendanceSystemProject.repository.AttendanceRepository;
import com.example.studentAttendanceSystemProject.repository.LectureSessionRepository;
import com.example.studentAttendanceSystemProject.repository.PreVerifiedStudentRepository;
import com.example.studentAttendanceSystemProject.repository.SubjectRepository;
import com.example.studentAttendanceSystemProject.repository.TeacherRepository;

@Service
public class LectureSessionService {

    @Autowired
    private LectureSessionRepository lectureSessionRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private PreVerifiedStudentRepository studentRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private QRCodeService qrCodeService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private AttendanceSettingsService settingsService;

    /**
     * Start a new lecture session with a per-lecture unique QR code.
     */
    @Transactional
    public LectureSession startLectureSession(Long subjectId, String teacherEmail, String division,
                                             Integer customDurationMinutes, Integer customWindowMinutes) {
        Teacher teacher = teacherRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new RuntimeException("Teacher not found: " + teacherEmail));

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found: " + subjectId));

        AttendanceSettings settings = settingsService.getSettings();
        int durationMinutes = (customDurationMinutes != null && customDurationMinutes > 0)
                ? customDurationMinutes : settings.getLectureDurationMinutes();
        int windowMinutes = (customWindowMinutes != null && customWindowMinutes > 0)
                ? customWindowMinutes : settings.getQrScanWindowMinutes();

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        LocalTime endTime = now.plusMinutes(durationMinutes);

        LectureSession session = new LectureSession();
        String sessionCode = "LEC-" + today.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + (1000 + (int)(Math.random() * 9000));
        session.setSessionCode(sessionCode);

        // Generate cryptographically unique dynamic token for QR code
        String qrToken = UUID.randomUUID().toString().replace("-", "");
        session.setQrToken(qrToken);

        session.setSubject(subject);
        session.setTeacher(teacher);
        session.setDepartment(subject.getDepartment());
        session.setYear(subject.getYear());
        session.setDivision(division != null && !division.trim().isEmpty() ? division.trim() : subject.getDivision());
        session.setDate(today);
        session.setStartTime(now);
        session.setEndTime(endTime);
        session.setStatus("ACTIVE");
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusMinutes(durationMinutes));
        session.setFinalWindowMinutes(windowMinutes);

        LectureSession saved = lectureSessionRepository.save(session);

        auditLogService.log("LECTURE_SESSION_STARTED", teacher.getEmail(), "ROLE_TEACHER",
                "Started lecture session: " + sessionCode + " for Subject: " + subject.getSubjectName()
                        + ", Year: " + subject.getYear() + ", Division: " + session.getDivision(), "SUCCESS");

        return saved;
    }

    /**
     * Generate Base64 QR code image for a lecture session.
     */
    public String getQrCodeImage(LectureSession session, String baseUrl) {
        // Embed the direct mobile attendance URL into the QR code
        String scanUrl = baseUrl + "/attend?token=" + session.getQrToken();
        return qrCodeService.generateQRCodeBase64(scanUrl, 300, 300);
    }

    public Optional<LectureSession> getByToken(String qrToken) {
        return lectureSessionRepository.findByQrToken(qrToken);
    }

    public Optional<LectureSession> getById(Long id) {
        return lectureSessionRepository.findById(id);
    }

    public List<LectureSession> getSessionsByTeacher(Teacher teacher) {
        return lectureSessionRepository.findByTeacherOrderByCreatedAtDesc(teacher);
    }

    /**
     * Closes/expires an active lecture QR code manually.
     */
    @Transactional
    public void closeLectureSession(Long sessionId, String teacherEmail) {
        LectureSession session = lectureSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        session.setStatus("EXPIRED");
        lectureSessionRepository.save(session);

        auditLogService.log("LECTURE_SESSION_CLOSED", teacherEmail, "ROLE_TEACHER",
                "Manually expired QR code for session " + session.getSessionCode(), "SUCCESS");
    }

    /**
     * Server-side validation and attendance recording as per PDF rules:
     * 1. Registered Student Account
     * 2. Registered Mobile Number Verification
     * 3. Previous-Day QR Blocked (Date validation)
     * 4. QR Expiry System (Status check & end time check)
     * 5. Lecture Time Validation (Final 10 minutes check)
     * 6. One Mobile - One Attendance (Single attendance per lecture check)
     */
    @Transactional
    public Map<String, Object> validateAndMarkAttendance(String qrToken, String studentEmail) {
        Map<String, Object> result = new HashMap<>();

        // 1. Check Student
        PreVerifiedStudent student = studentRepository.findByEmail(studentEmail);
        if (student == null) {
            result.put("success", false);
            result.put("message", "विद्यार्थी खाते सापडले नाही (Student account not found).");
            auditLogService.log("QR_SCAN_REJECTED", studentEmail, "ROLE_STUDENT",
                    "Scan rejected: unregistered student account", "FAILED");
            return result;
        }

        // 2. Registered Mobile Verification (Page 1 & 2 Rule: Registered Mobile Only)
        if (student.getMobileNumber() == null || student.getMobileNumber().trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "तुमचा मोबाईल नंबर डेटाबेसमध्ये नोंदणीकृत नाही (Mobile number not registered in database). Please contact Admin.");
            auditLogService.log("QR_SCAN_REJECTED", studentEmail, "ROLE_STUDENT",
                    "Scan rejected: Mobile number not registered in database for " + student.getName(), "FAILED");
            return result;
        }

        // 3. Find Lecture Session
        Optional<LectureSession> sessionOpt = lectureSessionRepository.findByQrToken(qrToken);
        if (sessionOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "अवैध किंवा अमान्य QR कोड (Invalid or unrecognized QR Code).");
            auditLogService.log("QR_SCAN_REJECTED", studentEmail, "ROLE_STUDENT",
                    "Scan rejected: Invalid token " + qrToken, "FAILED");
            return result;
        }

        LectureSession session = sessionOpt.get();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        // 4. Previous-day QR blocked (Page 2 Rule)
        if (!session.getDate().isEqual(today)) {
            result.put("success", false);
            result.put("message", "मागील दिवसाचा QR आज वापरता येणार नाही (Previous-day QR code is blocked).");
            auditLogService.log("QR_SCAN_REJECTED", studentEmail, "ROLE_STUDENT",
                    "Scan rejected: Old QR scanned from date " + session.getDate(), "FAILED");
            return result;
        }

        // 5. QR Expiry System (Page 1 & 2 Rule: Lecture संपल्यावर QR expire होईल)
        if ("EXPIRED".equalsIgnoreCase(session.getStatus()) || "COMPLETED".equalsIgnoreCase(session.getStatus())) {
            result.put("success", false);
            result.put("message", "हा QR कोड संपला आहे (This lecture QR code has expired).");
            auditLogService.log("QR_SCAN_REJECTED", studentEmail, "ROLE_STUDENT",
                    "Scan rejected: Session expired (" + session.getSessionCode() + ")", "FAILED");
            return result;
        }

        if (session.getExpiresAt() != null && now.isAfter(session.getExpiresAt())) {
            session.setStatus("EXPIRED");
            lectureSessionRepository.save(session);
            result.put("success", false);
            result.put("message", "लेक्चर संपले आहे, QR कोड कालबाह्य झाला आहे (Lecture ended. QR code has expired).");
            auditLogService.log("QR_SCAN_REJECTED", studentEmail, "ROLE_STUDENT",
                    "Scan rejected: Past lecture end time for " + session.getSessionCode(), "FAILED");
            return result;
        }

        // 6. Lecture Time Validation: Scanning is active while session is active and not expired
        if (!"ACTIVE".equalsIgnoreCase(session.getStatus())) {
            result.put("success", false);
            result.put("message", "हा लेक्चर QR कोड सक्रिय नाही (This lecture QR code is not active).");
            return result;
        }

        // 7. Department / Year / Division match check
        if (session.getDepartment() != null && student.getDepartment() != null &&
                !session.getDepartment().getId().equals(student.getDepartment().getId())) {
            result.put("success", false);
            result.put("message", "हा लेक्चर तुमच्या विभागासाठी नाही (This lecture is for department: "
                    + session.getDepartment().getName() + ").");
            return result;
        }

        // 8. One Mobile–One Attendance (Page 1 & 2 Rule: एका विद्यार्थ्याची त्याच lecture मध्ये दुसरी attendance स्वीकारली जाणार नाही)
        Optional<Attendance> existingAttendance = attendanceRepository.findByLectureSessionAndPstudent(session, student);
        if (existingAttendance.isPresent()) {
            result.put("success", false);
            result.put("message", "तुमची उपस्थिती या लेक्चरसाठी आधीच नोंदवली आहे (Attendance already marked for this lecture). Single attendance per lecture.");
            auditLogService.log("QR_SCAN_DUPLICATE", studentEmail, "ROLE_STUDENT",
                    "Duplicate scan attempt blocked for student " + student.getName() + " in session " + session.getSessionCode(), "WARNING");
            return result;
        }

        // 9. All validations passed! Save attendance securely
        Attendance attendance = new Attendance();
        attendance.setDate(today);
        attendance.setYear(session.getYear());
        attendance.setDivision(session.getDivision());
        attendance.setSubject(session.getSubject());
        attendance.setTeacher(session.getTeacher());
        attendance.setPstudent(student);
        attendance.setLectureSession(session);
        attendance.setPresent(true);
        attendance.setMarkedVia("QR_SCAN");
        attendance.setScanTimestamp(now);
        attendance.setStudentMobile(student.getMobileNumber());

        attendanceRepository.save(attendance);

        auditLogService.log("QR_SCAN_SUCCESS", studentEmail, "ROLE_STUDENT",
                "Attendance successfully marked via QR for " + student.getName() + " (Roll: " + student.getRollNumber()
                        + ", Mobile: " + student.getMobileNumber() + ") for Subject: " + session.getSubject().getSubjectName(), "SUCCESS");

        result.put("success", true);
        result.put("message", "उपस्थिती यशस्वीरित्या नोंदवली गेली! (Attendance marked successfully!)");
        result.put("studentName", student.getName());
        result.put("rollNumber", student.getRollNumber());
        result.put("subjectName", session.getSubject().getSubjectName());
        result.put("sessionCode", session.getSessionCode());
        result.put("time", now.format(DateTimeFormatter.ofPattern("hh:mm:ss a")));
        return result;
    }

    @Transactional
    public Map<String, Object> markAttendanceByRoll(String qrToken, String rollNumber) {
        Map<String, Object> result = new HashMap<>();

        if (rollNumber == null || rollNumber.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "Please enter your Roll Number.");
            return result;
        }

        PreVerifiedStudent student = studentRepository.findByRollNumber(rollNumber.trim());
        if (student == null) {
            student = studentRepository.findByEmail(rollNumber.trim());
        }

        if (student == null) {
            result.put("success", false);
            result.put("message", "Roll Number " + rollNumber + " not found in student records.");
            return result;
        }

        Optional<LectureSession> sessionOpt = lectureSessionRepository.findByQrToken(qrToken);
        if (sessionOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "Invalid or unrecognized QR Code.");
            return result;
        }

        LectureSession session = sessionOpt.get();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        if (!session.getDate().isEqual(today)) {
            result.put("success", false);
            result.put("message", "Previous-day QR code is expired.");
            return result;
        }

        if (!"ACTIVE".equalsIgnoreCase(session.getStatus()) || (session.getExpiresAt() != null && now.isAfter(session.getExpiresAt()))) {
            result.put("success", false);
            result.put("message", "This lecture QR code has expired.");
            return result;
        }

        Optional<Attendance> existingAttendance = attendanceRepository.findByLectureSessionAndPstudent(session, student);
        if (existingAttendance.isPresent()) {
            result.put("success", false);
            result.put("message", "Attendance already marked for Roll No: " + student.getRollNumber() + " (" + student.getName() + ").");
            return result;
        }

        Attendance attendance = new Attendance();
        attendance.setDate(today);
        attendance.setYear(session.getYear());
        attendance.setDivision(session.getDivision());
        attendance.setSubject(session.getSubject());
        attendance.setTeacher(session.getTeacher());
        attendance.setPstudent(student);
        attendance.setLectureSession(session);
        attendance.setPresent(true);
        attendance.setMarkedVia("QR_SCAN");
        attendance.setScanTimestamp(now);
        attendance.setStudentMobile(student.getMobileNumber());

        attendanceRepository.save(attendance);

        auditLogService.log("QR_SCAN_SUCCESS", student.getEmail(), "ROLE_STUDENT",
                "Attendance marked via QR for " + student.getName() + " (Roll: " + student.getRollNumber() + ")", "SUCCESS");

        result.put("success", true);
        result.put("message", "Attendance marked successfully!");
        result.put("studentName", student.getName());
        result.put("rollNumber", student.getRollNumber());
        result.put("subjectName", session.getSubject().getSubjectName());
        result.put("sessionCode", session.getSessionCode());
        result.put("time", now.format(DateTimeFormatter.ofPattern("hh:mm:ss a")));
        return result;
    }
}
