package com.example.studentAttendanceSystemProject.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.studentAttendanceSystemProject.entity.Attendance;
import com.example.studentAttendanceSystemProject.entity.PreVerifiedStudent;
import com.example.studentAttendanceSystemProject.entity.Subject;
import com.example.studentAttendanceSystemProject.entity.Teacher;
import com.example.studentAttendanceSystemProject.repository.AttendanceRepository;
import com.example.studentAttendanceSystemProject.repository.PreVerifiedStudentRepository;
import com.example.studentAttendanceSystemProject.repository.SubjectRepository;
import com.example.studentAttendanceSystemProject.service.AttendanceService;
import com.example.studentAttendanceSystemProject.service.SubjectService;
import com.example.studentAttendanceSystemProject.service.TeacherService;

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
    SubjectRepository subjectRepository;
    
    @Autowired
    AttendanceService attendanceService;
//    
//

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {

        // Logged-in teacher
        Teacher teacher = teacherService.getByEmail(principal.getName());

        // Subjects assigned to teacher
        List<Subject> subjects = subjectService.getByTeacher(teacher);

        // 🔥 Attendance summary list (THIS WAS MISSING)
        List<Map<String, Object>> studentAttendanceList = new ArrayList<>();

        for (Subject subject : subjects) {

            // Students of same department + year as subject
            List<PreVerifiedStudent> students =
                    studentRepository.findByDepartmentAndYear(
                            subject.getDepartment(), subject.getYear());

            // Total classes taken for this subject
            long totalClasses = attendanceRepository.countBySubject(subject);

            for (PreVerifiedStudent student : students) {

                long presentCount =
                        attendanceRepository.countByStudentAndSubject(student, subject);

                double percent =
                        totalClasses > 0 ? (presentCount * 100.0) / totalClasses : 0;

                Map<String, Object> map = new HashMap<>();
                map.put("student", student);
                map.put("subject", subject);
                map.put("attendancePercent", percent);
                map.put("lowAttendance", percent < 75);

                studentAttendanceList.add(map);
            }
        }

        // Send data to UI
        model.addAttribute("teacher", teacher);
        model.addAttribute("subjects", subjects);
        model.addAttribute("studentAttendanceList", studentAttendanceList);

        return "teacher/dashboard";
    }

    
   
    @GetMapping("/attendance/{subjectId}")
    public String attendanceForm(@PathVariable Long subjectId, Model model, Principal principal) {
        Teacher teacher = teacherService.getByEmail(principal.getName());
        Subject subject = subjectService.getById(subjectId);

        List<PreVerifiedStudent> students = studentRepository
                .findByDepartment_IdAndYear(subject.getDepartment().getId(), subject.getYear());

        // Load today's attendance
        LocalDate today = LocalDate.now();
        List<Attendance> attendances = attendanceRepository.findByTeacherAndSubjectAndDate(teacher, subject, today);

        // Convert to Map<studentId, present>
        Map<Long, Boolean> attendanceMap = new HashMap<>();
        for (Attendance a : attendances) {
            attendanceMap.put(a.getPstudent().getId(), a.isPresent());
        }

        model.addAttribute("teacher", teacher);
        model.addAttribute("subject", subject);
        model.addAttribute("students", students);
        model.addAttribute("attendanceMap", attendanceMap); // 🔹 Use map
        return "teacher/attendance-form";
    }

       

    
//    @GetMapping("/attendance/{subjectId}")
//    public String attendanceForm(@PathVariable Long subjectId, Model model, Principal principal) {
//        Teacher teacher = teacherService.getByEmail(principal.getName());
//        Subject subject = subjectService.getById(subjectId);
//
//        // 🔥 Filter by department + subject year
//        List<PreVerifiedStudent> students = studentRepository
//                .findByDepartment_IdAndYear(subject.getDepartment().getId(), subject.getYear());
//
//        model.addAttribute("teacher", teacher);
//        model.addAttribute("subject", subject);
//        model.addAttribute("students", students);
//        return "teacher/attendance-form";
//    }

    
    

//    @GetMapping("/attendance/{subjectId}")
//    public String attendanceForm(@PathVariable Long subjectId, Model model, Principal principal) {
//        Teacher teacher = teacherService.getByEmail(principal.getName());
//        Subject subject = subjectService.getById(subjectId);
//
//        // Students of the same department + year as subject
//        List<PreVerifiedStudent> students = studentRepository
//                .findByDepartment_IdAndYear(subject.getDepartment().getId(), subject.getYear());
//
//        // ✅ Get today's existing attendance records
//        LocalDate today = LocalDate.now();
//        List<Attendance> attendances = attendanceRepository.findByTeacherAndSubjectAndDate(teacher, subject, today);
//
//        model.addAttribute("teacher", teacher);
//        model.addAttribute("subject", subject);
//        model.addAttribute("students", students);
//        model.addAttribute("attendances", attendances);  // <-- send attendances to Thymeleaf
//
//        return "teacher/attendance-form";
//    }

    
//    @PostMapping("/attendance/save")
//    public String saveAttendance(
//            @RequestParam("subjectId") Long subjectId,
//            @RequestParam("studentIds") List<Long> studentIds,
//            @RequestParam(value = "presentList", required = false) List<String> presentList,
//            Principal principal) {
//
//        Teacher teacher = teacherService.getByEmail(principal.getName());
//        Subject subject = subjectService.getById(subjectId);
//        LocalDate today = LocalDate.now();
//
//        for (int i = 0; i < studentIds.size(); i++) {
//            PreVerifiedStudent student = studentRepository.findById(studentIds.get(i)).get();
//            boolean present = presentList != null && presentList.size() > i;
//
//            Attendance attendance = new Attendance();
//            attendance.setTeacher(teacher);
//            attendance.setPstudent(student);
//            attendance.setSubject(subject); // ya subject entity set kar sakte ho
//            attendance.setDate(today);
//            attendance.setPresent(present); // false agar unchecked
//
//            attendanceRepository.save(attendance);
//        }
//
//        return "redirect:/teacher/dashboard";
//    }

    
    
//    
//    @PostMapping("/attendance/save")
//    public String saveAttendance(
//            @RequestParam("subjectId") Long subjectId,
//            @RequestParam("studentIds") List<Long> studentIds,
//            @RequestParam(value = "presentList", required = false) List<String> presentList,
//            Principal principal) {
//
//        Teacher teacher = teacherService.getByEmail(principal.getName());
//        Subject subject = subjectService.getById(subjectId);
//        LocalDate today = LocalDate.now();
//
//        for (int i = 0; i < studentIds.size(); i++) {
//            PreVerifiedStudent student = studentRepository.findById(studentIds.get(i)).get();
//            boolean present = presentList != null && presentList.size() > i;
//
//            // ✅ Check if attendance already exists
//            Attendance existing = attendanceRepository
//                    .findByPstudentIdAndSubjectIdAndDateAndTeacherId(
//                            student.getId(), subject.getId(), today, teacher.getId()
//                    );
//
//            if (existing != null) {
//                // Update existing record
//                existing.setPresent(present);
//                attendanceRepository.save(existing);
//            } else {
//                // Insert new record
//                Attendance attendance = new Attendance();
//                attendance.setTeacher(teacher);
//                attendance.setPstudent(student);
//                attendance.setSubject(subject);
//                attendance.setDate(today);
//                attendance.setPresent(present);
//                attendanceRepository.save(attendance);
//            }
//        }
//
//        return "redirect:/teacher/dashboard";
//    }

    
//    @PostMapping("/attendance/save")
//    public String saveAttendance(
//            @RequestParam("subjectId") Long subjectId,
//            @RequestParam("studentIds") List<Long> studentIds,
//            @RequestParam(value = "presentList", required = false) List<String> presentList,
//            Principal principal) {
//
//        Teacher teacher = teacherService.getByEmail(principal.getName());
//        Subject subject = subjectService.getById(subjectId);
//        LocalDate today = LocalDate.now();
//
//        for (int i = 0; i < studentIds.size(); i++) {
//            PreVerifiedStudent student = studentRepository.findById(studentIds.get(i)).get();
//            boolean present = presentList != null && presentList.contains(student.getId().toString());
//
//            Attendance attendance = new Attendance();
//            attendance.setTeacher(teacher);
//            attendance.setPstudent(student);
//            attendance.setSubject(subject);
//            attendance.setDate(today);
//            attendance.setPresent(present);
//
//            // ✅ Use saveOrUpdate method from AttendanceService
//            attendanceService.saveOrUpdateAttendance(attendance);
//        }
//
//        return "redirect:/teacher/dashboard";
//    }


//    @PostMapping("/attendance/save")
//    public String saveAttendance(
//            @RequestParam("subjectId") Long subjectId,
//            @RequestParam("studentIds") List<Long> studentIds,
//            @RequestParam(value = "presentList", required = false) List<String> presentList,
//            Principal principal) {
//
//        Teacher teacher = teacherService.getByEmail(principal.getName());
//        Subject subject = subjectService.getById(subjectId);
//        LocalDate today = LocalDate.now();
//
//        for (int i = 0; i < studentIds.size(); i++) {
//            PreVerifiedStudent student = studentRepository.findById(studentIds.get(i)).get();
//            boolean present = presentList != null && presentList.contains(student.getId().toString());
//
//            // 🔥 Check if attendance already exists
//            Attendance existing = attendanceRepository
//                .findByPstudentIdAndSubjectIdAndDateAndTeacherId(
//                    student.getId(),
//                    subject.getId(),
//                    today,
//                    teacher.getId()
//                );
//
//            if (existing != null) {
//                // update only present/absent
//                existing.setPresent(present);
//                attendanceRepository.save(existing);
//            } else {
//                // create new attendance if not exist
//                Attendance attendance = new Attendance();
//                attendance.setTeacher(teacher);
//                attendance.setPstudent(student);
//                attendance.setSubject(subject);
//                attendance.setDate(today);
//                attendance.setPresent(present);
//                attendanceRepository.save(attendance);
//            }
//        }
//
//        return "redirect:/teacher/dashboard";
//    }

    
//    @PostMapping("/attendance/save")
//    public String saveAttendance(
//            @RequestParam("subjectId") Long subjectId,
//            @RequestParam("studentIds") List<Long> studentIds,
//            @RequestParam(value = "presentList", required = false) List<String> presentList,
//            Principal principal) {
//
//        Teacher teacher = teacherService.getByEmail(principal.getName());
//        Subject subject = subjectService.getById(subjectId);
//        LocalDate today = LocalDate.now();
//
//        // 🔹 Ye for loop yahan rakho
//        for (Long studentId : studentIds) {
//            PreVerifiedStudent student = studentRepository.findById(studentId).get();
//            boolean present = presentList != null && presentList.contains(student.getId().toString());
//
//            Attendance existing = attendanceRepository
//                .findByPstudentIdAndSubjectIdAndDateAndTeacherId(
//                    student.getId(),
//                    subject.getId(),
//                    today,
//                    teacher.getId()
//                );
//
//            if (existing != null) {
//                existing.setPresent(present); // update only present/absent
//                attendanceRepository.save(existing);
//            } else {
//                Attendance attendance = new Attendance();
//                attendance.setTeacher(teacher);
//                attendance.setPstudent(student);
//                attendance.setSubject(subject);
//                attendance.setDate(today);
//                attendance.setPresent(present);
//                attendanceRepository.save(attendance);
//            }
//        }
//
//        return "redirect:/teacher/dashboard";
//    }

    
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

            // 🔹 Check if attendance already exists
            Attendance existing = attendanceRepository
                .findByPstudentIdAndSubjectIdAndDateAndTeacherId(
                    student.getId(),
                    subject.getId(),
                    today,
                    teacher.getId()
                );

            if (existing != null) {
                existing.setPresent(present); // update existing
                attendanceRepository.save(existing);
            } else {
                Attendance attendance = new Attendance();
                attendance.setTeacher(teacher);
                attendance.setPstudent(student);
                attendance.setSubject(subject);
                attendance.setDate(today);
                attendance.setPresent(present);
                attendanceRepository.save(attendance);
            }
        }

        return "redirect:/teacher/dashboard";
    }

    
    @GetMapping("/attendance/report")
    public String attendanceReport(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model,
            Principal principal) {

        Teacher teacher = teacherService.getByEmail(principal.getName());
        List<Attendance> attendances = new ArrayList<>();

        if(subjectId != null && date != null){
            Subject subject = subjectService.getById(subjectId);
            attendances = attendanceRepository.findByTeacherAndSubjectAndDate(
                    teacher, subject, date);
        } else {
            // Optional: show all attendances for teacher if filter not applied
            attendances = attendanceRepository.findByTeacher(teacher);
        }

        model.addAttribute("attendances", attendances);
        model.addAttribute("subjects", subjectRepository.findByTeacher(teacher));
        model.addAttribute("teacher", teacher);

        return "teacher/attendance-report";
    }
    
    



}
