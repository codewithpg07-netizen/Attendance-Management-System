package com.example.studentAttendanceSystemProject.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import com.example.studentAttendanceSystemProject.entity.User;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Autowired
	private CustomUserDetailsService userDetailsService;

	@Autowired
	private com.example.studentAttendanceSystemProject.service.OtpService otpService;

	@Autowired
	private com.example.studentAttendanceSystemProject.service.AttendanceSettingsService settingsService;

	
	 @Bean
	    public PasswordEncoder passwordEncoder() {
	        return new BCryptPasswordEncoder();
	 }
	 
	 @Bean
	    public DaoAuthenticationProvider authProvider() {
	        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
	        provider.setUserDetailsService(userDetailsService);
	        provider.setPasswordEncoder(passwordEncoder());
	        return provider;
	    }


	 @Bean
	    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
	        http
	            .csrf().disable()
	            .authorizeHttpRequests()
	            .requestMatchers("/", "/register", "/login", "/verify-otp", "/resend-otp", "/css/**","/images/**", "/js/**", "/attend/**").permitAll()
	            .requestMatchers("/admin/**").hasRole("ADMIN")
	            .requestMatchers("/teacher/**").hasRole("TEACHER")
	            .requestMatchers("/student/**").hasRole("STUDENT")
	            .anyRequest().authenticated()
	            .and()
	            .formLogin()
	            .loginPage("/login")
	            .successHandler((request, response, authentication) -> {
	                String role = authentication.getAuthorities().iterator().next().getAuthority();
	                String email = authentication.getName();
	                request.getSession().setAttribute("AUTHENTICATED_ROLE", role);
	                request.getSession().setAttribute("AUTHENTICATED_EMAIL", email);

	                boolean otpActive = settingsService.getSettings().isOtpEnabled();
	                if (otpActive) {
	                    request.getSession().setAttribute("OTP_PENDING", true);
	                    String generatedOtp = otpService.generateOtp(email, role);
	                    request.getSession().setAttribute("DEMO_OTP", generatedOtp);
	                    response.sendRedirect("/verify-otp");
	                } else {
	                    if(role.equals("ROLE_ADMIN")) response.sendRedirect("/admin/dashboard");
	                    else if(role.equals("ROLE_TEACHER")) response.sendRedirect("/teacher/dashboard");
	                    else response.sendRedirect("/student/dashboard");
	                }
	            })
	            .permitAll()
	            .and()
	            .logout().permitAll();

	        return http.build();
	    }
	            
@Bean
public UserDetailsService userDetailsService() {
    return userDetailsService;
}
}
