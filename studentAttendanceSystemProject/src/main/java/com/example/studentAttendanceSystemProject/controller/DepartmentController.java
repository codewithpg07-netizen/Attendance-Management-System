package com.example.studentAttendanceSystemProject.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.studentAttendanceSystemProject.entity.Department;
import com.example.studentAttendanceSystemProject.service.DepartmentService;

@Controller
@RequestMapping("/admin/departments")
public class DepartmentController {

	
	 @Autowired
	    private DepartmentService service;

	 @GetMapping
	    public String list(Model model) {
	        model.addAttribute("departments", service.getAll());
	        model.addAttribute("department", new Department());
	        return "admin/departments";
	    }

	    @PostMapping("/save")
	    public String save(@ModelAttribute Department department) {
	        service.save(department);
	        return "redirect:/admin/departments";
	    }

	    @GetMapping("/delete/{id}")
	    public String delete(@PathVariable Long id) {
	        service.delete(id);
	        return "redirect:/admin/departments";
	    }    
	    
}
