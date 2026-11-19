package com.example.studentAttendanceSystemProject.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.studentAttendanceSystemProject.entity.Department;
import com.example.studentAttendanceSystemProject.entity.PreVerifiedStudent;
import com.example.studentAttendanceSystemProject.entity.User;
import com.example.studentAttendanceSystemProject.repository.DepartmentRepository;
import com.example.studentAttendanceSystemProject.repository.PreVerifiedStudentRepository;
import com.example.studentAttendanceSystemProject.repository.UserRepository;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PreVerifiedStudentRepository preVerifiedStudentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Override
    public void addTeacher(User teacher) {
        teacher.setRole("TEACHER");
        teacher.setPassword(passwordEncoder.encode(teacher.getPassword()));
        userRepository.save(teacher);
    }

    @Override
    public List<User> getAllTeachers() {
        return userRepository.findAll().stream()
                .filter(u -> "TEACHER".equals(u.getRole()))
                .toList();
    }

    @Override
    public List<PreVerifiedStudent> getAllStudents() {
        return preVerifiedStudentRepository.findAll();
    }

    @Override
    public void uploadStudentList(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            System.out.println("File is empty!");
            return;
        }

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";

        if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
            uploadExcelStudentList(file);
        } else {
            uploadCsvStudentList(file);
        }
    }

    private void uploadExcelStudentList(MultipartFile file) throws Exception {
        List<Department> allDepts = departmentRepository.findAll();
        Department defaultDept = !allDepts.isEmpty() ? allDepts.get(0) : null;
        DataFormatter formatter = new DataFormatter();

        String fname = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        String defaultYear = fname.toUpperCase().contains("TY") ? "3" : (fname.toUpperCase().contains("SY") ? "2" : "1");
        String defaultDiv = fname.toUpperCase().contains("B") ? "B" : "A";

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            boolean headerSkipped = false;

            for (Row row : sheet) {
                if (!headerSkipped) {
                    headerSkipped = true; // skip header
                    continue;
                }

                String col0 = formatter.formatCellValue(row.getCell(0)).trim();
                String col1 = formatter.formatCellValue(row.getCell(1)).trim();
                String col2 = formatter.formatCellValue(row.getCell(2)).trim();
                String col3 = formatter.formatCellValue(row.getCell(3)).trim();

                if (col0.isEmpty() && col1.isEmpty()) {
                    continue;
                }

                String rollNumber = col0;
                String name = col1;
                String mobile = col2.replaceAll("\\s+", "");
                String parentMobile = col3.replaceAll("\\s+", "");
                String email = "student" + rollNumber + "@college.edu";
                String year = defaultYear;
                String division = defaultDiv;
                String section = defaultDiv;

                PreVerifiedStudent existingStudent = preVerifiedStudentRepository
                        .findByRollNumberOrEmail(rollNumber, email);

                if (existingStudent != null) {
                    existingStudent.setName(name);
                    if (defaultDept != null) existingStudent.setDepartment(defaultDept);
                    existingStudent.setYear(year);
                    existingStudent.setSection(section);
                    existingStudent.setDivision(division);
                    if (!mobile.isEmpty()) existingStudent.setMobileNumber(mobile);
                    if (!parentMobile.isEmpty()) existingStudent.setParentMobileNumber(parentMobile);
                    preVerifiedStudentRepository.save(existingStudent);
                } else {
                    PreVerifiedStudent s = new PreVerifiedStudent();
                    s.setRollNumber(rollNumber);
                    s.setName(name);
                    s.setEmail(email);
                    s.setDepartment(defaultDept);
                    s.setYear(year);
                    s.setSection(section);
                    s.setDivision(division);
                    s.setMobileNumber(mobile);
                    s.setParentMobileNumber(parentMobile);
                    s.setRegistered(false);
                    preVerifiedStudentRepository.save(s);
                }
            }
        }
    }

    private void uploadCsvStudentList(MultipartFile file) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean firstLine = true;
            List<Department> depts = departmentRepository.findAll();
            Department defaultDept = !depts.isEmpty() ? depts.get(0) : null;

            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false; // skip header
                    continue;
                }

                String[] data = line.split(",");
                if (data.length < 2) {
                    continue;
                }

                String rollNumber = data[0].trim();
                String name = data[1].trim();

                // If 4-column dataset: Roll, Name, Mobile, Parent Mobile
                if (data.length == 4) {
                    String mobile = data[2].trim().replaceAll("\\s+", "");
                    String parentMobile = data[3].trim().replaceAll("\\s+", "");
                    String email = "student" + rollNumber + "@college.edu";

                    PreVerifiedStudent existing = preVerifiedStudentRepository.findByRollNumberOrEmail(rollNumber, email);
                    if (existing != null) {
                        existing.setName(name);
                        existing.setMobileNumber(mobile);
                        existing.setParentMobileNumber(parentMobile);
                        preVerifiedStudentRepository.save(existing);
                    } else {
                        PreVerifiedStudent s = new PreVerifiedStudent();
                        s.setRollNumber(rollNumber);
                        s.setName(name);
                        s.setEmail(email);
                        s.setDepartment(defaultDept);
                        s.setYear("3");
                        s.setDivision("A");
                        s.setSection("A");
                        s.setMobileNumber(mobile);
                        s.setParentMobileNumber(parentMobile);
                        s.setRegistered(false);
                        preVerifiedStudentRepository.save(s);
                    }
                    continue;
                }

                // If 5+ columns format: Roll, Name, Email, Dept, Year, Section, Division, Mobile, ParentMobile
                String email = data.length > 2 ? data[2].trim() : ("student" + rollNumber + "@college.edu");
                String deptName = data.length > 3 ? data[3].trim() : "";
                Department dept = departmentRepository.findByName(deptName);
                if (dept == null && defaultDept != null) {
                    dept = defaultDept;
                }

                String year = data.length > 4 ? data[4].trim() : "3";
                String section = data.length > 5 ? data[5].trim() : "A";
                String division = data.length > 6 ? data[6].trim() : section;
                String mobile = data.length > 7 ? data[7].trim().replaceAll("\\s+", "") : "";
                String parentMobile = data.length > 8 ? data[8].trim().replaceAll("\\s+", "") : "";

                PreVerifiedStudent existingStudent = preVerifiedStudentRepository
                        .findByRollNumberOrEmail(rollNumber, email);

                if (existingStudent != null) {
                    existingStudent.setName(name);
                    if (dept != null) existingStudent.setDepartment(dept);
                    existingStudent.setYear(year);
                    existingStudent.setSection(section);
                    existingStudent.setDivision(division);
                    if (!mobile.isEmpty()) existingStudent.setMobileNumber(mobile);
                    if (!parentMobile.isEmpty()) existingStudent.setParentMobileNumber(parentMobile);
                    preVerifiedStudentRepository.save(existingStudent);
                } else {
                    PreVerifiedStudent s = new PreVerifiedStudent();
                    s.setRollNumber(rollNumber);
                    s.setName(name);
                    s.setEmail(email);
                    s.setDepartment(dept);
                    s.setYear(year);
                    s.setSection(section);
                    s.setDivision(division);
                    s.setMobileNumber(mobile);
                    s.setParentMobileNumber(parentMobile);
                    s.setRegistered(false);
                    preVerifiedStudentRepository.save(s);
                }
            }
        }
    }

    @Override
    public PreVerifiedStudent getStudentById(Long id) {
        return preVerifiedStudentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
    }

    @Override
    public void deleteStudent(Long id) {
        preVerifiedStudentRepository.deleteById(id);
    }

    @Override
    public PreVerifiedStudent saveStudent(PreVerifiedStudent student) {
        return preVerifiedStudentRepository.save(student);
    }

    @Override
    public List<PreVerifiedStudent> getFilteredStudents(Long departmentId, String year) {
        List<PreVerifiedStudent> filteredStudents;

        if (departmentId != null && year != null && !year.isEmpty()) {
            filteredStudents = preVerifiedStudentRepository.findByDepartmentIdAndYear(departmentId, year);
        } else if (departmentId != null) {
            filteredStudents = preVerifiedStudentRepository.findByDepartmentId(departmentId);
        } else if (year != null && !year.isEmpty()) {
            filteredStudents = preVerifiedStudentRepository.findByYear(year);
        } else {
            filteredStudents = preVerifiedStudentRepository.findAll();
        }

        // Deduplicate by Roll Number (fallback to Email)
        Map<String, PreVerifiedStudent> uniqueMap = new LinkedHashMap<>();
        for (PreVerifiedStudent student : filteredStudents) {
            String key = (student.getRollNumber() != null && !student.getRollNumber().trim().isEmpty())
                    ? student.getRollNumber().trim()
                    : student.getEmail();
            if (key != null) {
                uniqueMap.put(key, student);
            }
        }

        return new ArrayList<>(uniqueMap.values());
    }
}
