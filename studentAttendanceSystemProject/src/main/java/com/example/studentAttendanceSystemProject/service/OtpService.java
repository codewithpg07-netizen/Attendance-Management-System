package com.example.studentAttendanceSystemProject.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OtpService {

    @Autowired
    private AuditLogService auditLogService;

    // In-memory cache for OTPs: key = emailOrMobile, value = OtpData
    private static class OtpData {
        String code;
        LocalDateTime expiryTime;
        String role;

        OtpData(String code, LocalDateTime expiryTime, String role) {
            this.code = code;
            this.expiryTime = expiryTime;
            this.role = role;
        }
    }

    private final Map<String, OtpData> otpStorage = new ConcurrentHashMap<>();

    public String generateOtp(String identifier, String role) {
        String code = String.format("%06d", new Random().nextInt(900000) + 100000);
        // Valid for 5 minutes
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);
        otpStorage.put(identifier.toLowerCase().trim(), new OtpData(code, expiry, role));

        auditLogService.log("OTP_GENERATED", identifier, role,
                "6-digit OTP generated for login verification", "SUCCESS");

        return code;
    }

    public boolean validateOtp(String identifier, String code) {
        if (identifier == null || code == null) {
            return false;
        }
        String key = identifier.toLowerCase().trim();
        OtpData data = otpStorage.get(key);

        if (data == null) {
            auditLogService.log("OTP_VERIFICATION_FAILED", identifier, "UNKNOWN",
                    "No active OTP found for identifier", "FAILED");
            return false;
        }

        if (LocalDateTime.now().isAfter(data.expiryTime)) {
            otpStorage.remove(key);
            auditLogService.log("OTP_VERIFICATION_FAILED", identifier, data.role,
                    "OTP has expired", "FAILED");
            return false;
        }

        if (data.code.equals(code.trim())) {
            otpStorage.remove(key);
            auditLogService.log("OTP_LOGIN_SUCCESS", identifier, data.role,
                    "OTP successfully verified. User authenticated.", "SUCCESS");
            return true;
        }

        auditLogService.log("OTP_VERIFICATION_FAILED", identifier, data.role,
                "Invalid OTP code entered", "FAILED");
        return false;
    }

    public String getActiveOtpForTesting(String identifier) {
        OtpData data = otpStorage.get(identifier.toLowerCase().trim());
        if (data != null && !LocalDateTime.now().isAfter(data.expiryTime)) {
            return data.code;
        }
        return null;
    }
}
