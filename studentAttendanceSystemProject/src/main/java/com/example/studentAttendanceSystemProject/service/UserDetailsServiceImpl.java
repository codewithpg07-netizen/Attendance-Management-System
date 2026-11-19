package com.example.studentAttendanceSystemProject.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.studentAttendanceSystemProject.entity.User;
import com.example.studentAttendanceSystemProject.repository.UserRepository;

@Service
public class UserDetailsServiceImpl implements UserDetailService {

	
	private  UserRepository userRepository;

	

	
}
