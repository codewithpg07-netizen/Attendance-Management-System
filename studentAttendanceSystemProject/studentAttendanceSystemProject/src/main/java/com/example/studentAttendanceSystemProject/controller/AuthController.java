package com.example.studentAttendanceSystemProject.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.studentAttendanceSystemProject.entity.PreVerifiedStudent;
import com.example.studentAttendanceSystemProject.entity.User;
import com.example.studentAttendanceSystemProject.repository.PreVerifiedStudentRepository;
import com.example.studentAttendanceSystemProject.repository.UserRepository;

@Controller
public class AuthController {

	
	 @Autowired
	    private UserRepository userRepository;

	 @Autowired
	    private PreVerifiedStudentRepository preVerifiedStudentRepository;

	    @Autowired
	    private PasswordEncoder passwordEncoder;
	    
	    @GetMapping("/")
	    public String home() {
	        return "index"; // home.html
	    }
	    
	    
//	    registration page
	    @GetMapping("/register")
	    public String registerPage() {
	        return "register";
	    }

	    // Handle Student Registration
	    @PostMapping("/register")
	    public String registerStudent(@RequestParam String name,
	                                  @RequestParam String email,
	                                  @RequestParam String rollNumber,
	                                  @RequestParam String password,
	                                  Model model) {

	        PreVerifiedStudent verified = preVerifiedStudentRepository.findByEmailAndRollNumber(email, rollNumber);

	        if (verified == null) {
	            model.addAttribute("error", "You are not authorized student. Contact Admin.");
	            return "register";
	        }
	        if (verified.isRegistered()) {
	            model.addAttribute("error", "Already registered.");
	            return "register";
	        }

	        User user = new User();
	        user.setName(name);
	        user.setEmail(email);
	        user.setPassword(passwordEncoder.encode(password));
//	        user.setRole("STUDENT");
	        user.setRole("ROLE_STUDENT");

	        userRepository.save(user);

	        verified.setRegistered(true);
	        preVerifiedStudentRepository.save(verified);
	        
	        model.addAttribute("success", "Registration successful! Please login.");
	        return "login";
	    }

	    // Login Page
	    @GetMapping("/login")
	    public String loginPage() {
	        return "login";
	    }
	    
	    
//	    // Handle Login
//	    @PostMapping("/login")
//	    public String loginUser(@RequestParam String email,
//	                            @RequestParam String password,
//	                            Model model) {
//	        User user = userRepository.findByEmail(email);
//
//	        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
//	            switch (user.getRole()) {
//	                case "ADMIN":
//	                    return "redirect:/admin/dashboard";
//	                    
//	                case "TEACHER":
//	                    return "redirect:/teacher/dashboard";
//	                case "STUDENT":
//	                    return "redirect:/student/dashboard";
//	            }
//	        }
//	        model.addAttribute("error", "Invalid email or password");
//	        return "login";
//	    }
	    
	    // Admin Upload Student List
	

}



