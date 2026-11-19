package com.example.studentAttendanceSystemProject.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.studentAttendanceSystemProject.entity.Department;
import com.example.studentAttendanceSystemProject.entity.Subject;
import com.example.studentAttendanceSystemProject.entity.Teacher;
import com.example.studentAttendanceSystemProject.service.DepartmentService;
import com.example.studentAttendanceSystemProject.service.SubjectService;
import com.example.studentAttendanceSystemProject.service.TeacherService;

@Controller
@RequestMapping("/admin/subjects")
public class SubjectController {
	
	@Autowired
    private SubjectService subjectService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    TeacherService teacherService;
    @GetMapping
    public String list(Model model) {
        model.addAttribute("subjects", subjectService.getAll());
        model.addAttribute("departments", departmentService.getAll());
        model.addAttribute("teachers", teacherService.getAllTeachers()); // <- ye teacher list
        model.addAttribute("subject", new Subject());
        return "admin/subjects";
    }
    


//    
//    @PostMapping("/save")
//    public String save(@ModelAttribute Subject subject) {
//
//    	Teacher teacher = teacherService.getById(subject.getTeacher().getId());
//        subject.setTeacher(teacher);
//        
//        // Fetch the full Department object from DB using selected id
//        Department dept = departmentService.getById(subject.getDepartment().getId());
//        subject.setDepartment(dept); // set proper object
//
//
//        
//        subjectService.save(subject); // save Subject
//        return "redirect:/admin/subjects";
//    }
    
    @PostMapping("/save")
    public String save(@ModelAttribute Subject subject) {
        // Department
        Department dept = departmentService.getById(subject.getDepartment().getId());
        subject.setDepartment(dept);

        // Teacher
        if(subject.getTeacher() != null && subject.getTeacher().getId() != null){
            Teacher teacher = teacherService.getById(subject.getTeacher().getId());
            subject.setTeacher(teacher);
        }

        // Year is already set via form
        subjectService.save(subject);
        return "redirect:/admin/subjects";
    }


    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        subjectService.delete(id);
        return "redirect:/admin/subjects";
    }
    
 // Show edit form
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        Subject subject = subjectService.getById(id); // DB se existing subject
        if(subject == null) {
            return "redirect:/admin/subjects";
        }

        // Make sure department object is not null
        if(subject.getDepartment() == null) {
            subject.setDepartment(new Department());
        }

        model.addAttribute("subject", subject); // ye form me pre-fill hoga
        model.addAttribute("departments", departmentService.getAll()); // dropdown
        model.addAttribute("subjects", subjectService.getAll()); // table
        return "admin/subjects"; // same page me form + table
    }


    // Handle update
    @PostMapping("/update")
    public String update(@ModelAttribute Subject subject) {
        Department dept = departmentService.getById(subject.getDepartment().getId());
        subject.setDepartment(dept);

        

        // 🔥 teacher set (THIS IS MISSING)
        if (subject.getTeacher() != null &&
            subject.getTeacher().getId() != null) {

            Teacher teacher =
                teacherService.getById(subject.getTeacher().getId());
            subject.setTeacher(teacher);
        }

        
        subjectService.save(subject); // Save updates existing record
        return "redirect:/admin/subjects";
    }

    
    // 🔥 Ye naya method add karo yaha
    @GetMapping("/teachers-by-department/{deptId}")
    @ResponseBody
    public List<Teacher> getTeachersByDepartment(@PathVariable Long deptId) {
        return teacherService.getAllTeachers().stream()
                .filter(t -> t.getDepartment() != null && t.getDepartment().getId().equals(deptId))
                .collect(Collectors.toList());
    }


}
