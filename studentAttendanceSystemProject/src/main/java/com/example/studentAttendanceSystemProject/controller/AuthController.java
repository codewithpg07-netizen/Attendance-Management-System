package com.example.studentAttendanceSystemProject.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.studentAttendanceSystemProject.entity.PreVerifiedStudent;
import com.example.studentAttendanceSystemProject.entity.User;
import com.example.studentAttendanceSystemProject.repository.PreVerifiedStudentRepository;
import com.example.studentAttendanceSystemProject.repository.UserRepository;
import com.example.studentAttendanceSystemProject.service.AuditLogService;
import com.example.studentAttendanceSystemProject.service.OtpService;

import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PreVerifiedStudentRepository preVerifiedStudentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OtpService otpService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private com.example.studentAttendanceSystemProject.service.LectureSessionService lectureSessionService;

    @GetMapping("/")
    public String home() {
        return "index"; // home.html
    }

    // Registration page
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    // Handle Student Registration with Mobile and Parent Mobile Verification
    @PostMapping("/register")
    public String registerStudent(@RequestParam String name,
                                  @RequestParam String email,
                                  @RequestParam String rollNumber,
                                  @RequestParam(required = false) String mobileNumber,
                                  @RequestParam(required = false) String parentMobileNumber,
                                  @RequestParam(required = false) String division,
                                  @RequestParam String password,
                                  Model model) {

        PreVerifiedStudent verified = preVerifiedStudentRepository.findByEmailAndRollNumber(email, rollNumber);

        if (verified == null) {
            model.addAttribute("error", "You are not an authorized student in the college database. Contact Admin.");
            auditLogService.log("STUDENT_REGISTRATION_FAILED", email, "ROLE_STUDENT",
                    "Unauthorized registration attempt for Roll: " + rollNumber, "FAILED");
            return "register";
        }
        if (verified.isRegistered()) {
            model.addAttribute("error", "This student account is already registered. Please login.");
            return "register";
        }

        // Save User
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("ROLE_STUDENT");
        if (mobileNumber != null && !mobileNumber.trim().isEmpty()) {
            user.setMobileNumber(mobileNumber.trim());
        } else if (verified.getMobileNumber() != null) {
            user.setMobileNumber(verified.getMobileNumber());
        }
        userRepository.save(user);

        // Update PreVerifiedStudent with mobile, parent mobile, division
        verified.setRegistered(true);
        if (mobileNumber != null && !mobileNumber.trim().isEmpty()) {
            verified.setMobileNumber(mobileNumber.trim());
        }
        if (parentMobileNumber != null && !parentMobileNumber.trim().isEmpty()) {
            verified.setParentMobileNumber(parentMobileNumber.trim());
        }
        if (division != null && !division.trim().isEmpty()) {
            verified.setDivision(division.trim());
        }
        preVerifiedStudentRepository.save(verified);

        auditLogService.log("STUDENT_REGISTERED", email, "ROLE_STUDENT",
                "Student " + name + " (Roll: " + rollNumber + ", Mobile: " + verified.getMobileNumber() + ") registered successfully", "SUCCESS");

        model.addAttribute("success", "Registration successful! Please login.");
        return "login";
    }

    // Login Page
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // Module 2: OTP Verification Page
    @GetMapping("/verify-otp")
    public String verifyOtpPage(HttpSession session, Model model) {
        String email = (String) session.getAttribute("AUTHENTICATED_EMAIL");
        if (email == null) {
            return "redirect:/login";
        }
        String demoOtp = (String) session.getAttribute("DEMO_OTP");
        String role = (String) session.getAttribute("AUTHENTICATED_ROLE");

        model.addAttribute("email", email);
        model.addAttribute("demoOtp", demoOtp);
        model.addAttribute("role", role);
        return "verify-otp";
    }

    // Handle OTP Submission
    @PostMapping("/verify-otp")
    public String processOtp(@RequestParam("otp") String otp, HttpSession session, Model model) {
        String email = (String) session.getAttribute("AUTHENTICATED_EMAIL");
        String role = (String) session.getAttribute("AUTHENTICATED_ROLE");

        if (email == null || role == null) {
            return "redirect:/login";
        }

        boolean valid = otpService.validateOtp(email, otp);
        if (valid) {
            session.removeAttribute("OTP_PENDING");
            session.removeAttribute("DEMO_OTP");

            if ("ROLE_ADMIN".equals(role)) {
                return "redirect:/admin/dashboard";
            } else if ("ROLE_TEACHER".equals(role)) {
                return "redirect:/teacher/dashboard";
            } else {
                return "redirect:/student/dashboard";
            }
        } else {
            model.addAttribute("error", "अवैध किंवा कालबाह्य OTP कोड (Invalid or expired OTP code). Please try again.");
            model.addAttribute("email", email);
            model.addAttribute("demoOtp", otpService.getActiveOtpForTesting(email));
            return "verify-otp";
        }
    }

    // Resend OTP
    @GetMapping("/resend-otp")
    public String resendOtp(HttpSession session) {
        String email = (String) session.getAttribute("AUTHENTICATED_EMAIL");
        String role = (String) session.getAttribute("AUTHENTICATED_ROLE");
        if (email != null && role != null) {
            String newOtp = otpService.generateOtp(email, role);
            session.setAttribute("DEMO_OTP", newOtp);
        }
        return "redirect:/verify-otp?resent=true";
    }

    // ====== DIRECT MOBILE QR ATTENDANCE ======

    @GetMapping("/attend")
    public String mobileAttendPage(@RequestParam(required = false) String token, Model model, java.security.Principal principal) {
        String cleanToken = token != null ? token.trim() : "";
        model.addAttribute("token", cleanToken);

        if (!cleanToken.isEmpty()) {
            java.util.Optional<com.example.studentAttendanceSystemProject.entity.LectureSession> sessionOpt = lectureSessionService.getByToken(cleanToken);
            if (sessionOpt.isPresent()) {
                model.addAttribute("lectureSession", sessionOpt.get());
            }
        }

        if (principal != null) {
            PreVerifiedStudent student = preVerifiedStudentRepository.findByEmail(principal.getName());
            if (student != null) {
                model.addAttribute("loggedInStudent", student);
            }
        }

        return "attend";
    }

    @PostMapping("/attend")
    public String processMobileAttend(
            @RequestParam("token") String token,
            @RequestParam("rollNumber") String rollNumber,
            Model model) {

        java.util.Map<String, Object> res = lectureSessionService.markAttendanceByRoll(token, rollNumber);
        model.addAttribute("result", res);
        model.addAttribute("token", token);

        java.util.Optional<com.example.studentAttendanceSystemProject.entity.LectureSession> sessionOpt = lectureSessionService.getByToken(token);
        sessionOpt.ifPresent(s -> model.addAttribute("lectureSession", s));

        return "attend";
    }
}
