package com.example.studentAttendanceSystemProject.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.studentAttendanceSystemProject.entity.User;
import com.example.studentAttendanceSystemProject.repository.UserRepository;

import jakarta.annotation.PostConstruct;

@Component
public class AdminInitializer {

	    @Autowired
	    UserRepository userRepository;

	    @Autowired
	    PasswordEncoder encoder;

	    @PostConstruct
	    public void createAdmin() {
	        if (!userRepository.existsByEmail("admin@example.com")) {
	            User admin = new User();
	            admin.setEmail("admin@example.com");
	            admin.setName("Admin");
	            admin.setPassword(encoder.encode("admin123"));
	            admin.setRole("ROLE_ADMIN");
	            userRepository.save(admin);
	        }
	    }
	
}
